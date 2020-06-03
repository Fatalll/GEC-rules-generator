import json
from datetime import datetime
from multiprocessing.spawn import freeze_support
from typing import Set, List
from multiprocessing import Pool
import re

from rules import Token, Rule, precision
from templates import template


def load_sentences():
    sentences = []
    with open("sentences_beta.data", encoding="utf-8") as errors:
        for line in errors:
            tokens = []
            for error in line.split('#'):
                match = re.match("\\(\'(.+)\', (.+), ([A-Z.,\\-№'`$:]+), ([A-Z№\\-]+), corr: ([A-Za-z]*)\\)", error)
                if match is None:
                    print(error)
                    print(line)
                token = Token(match.group(1), match.group(2), match.group(3), match.group(4))
                token.correction = match.group(5).lower()
                token.index_in_sentence = len(tokens)
                token.index_in_block = len(sentences)
                tokens.append(token)

            sentences.append(tokens)
    return sentences


def find_errors(sentences):
    errors = []
    for sentence in sentences:
        for token in sentence:
            if token.correction != "":
                errors.append((sentence, token))
    return errors


sentences = load_sentences()
errors = find_errors(sentences)


def proto_rule_gen():
    id_counter = 0
    already_generated = set()
    for sentence, token in errors:
        if token.index_in_sentence > 3:
            for (description, before, after) in template(4, sentence[token.index_in_sentence - 4], sentence[token.index_in_sentence - 3], sentence[token.index_in_sentence - 2], sentence[token.index_in_sentence - 1], token):
                id_counter += 1
                rule = Rule(id_counter, description, before, after, 4, [])
                if rule.__only_rule_repr__() not in already_generated:
                    already_generated.add(rule.__only_rule_repr__())
                    yield rule

        if token.index_in_sentence > 2 and token.index_in_sentence + 1 < len(sentence):
            for (description, before, after) in template(3, sentence[token.index_in_sentence - 3], sentence[token.index_in_sentence - 2], sentence[token.index_in_sentence - 1], token, sentence[token.index_in_sentence + 1]):
                id_counter += 1
                rule = Rule(id_counter, description, before, after, 3, [])
                if rule.__only_rule_repr__() not in already_generated:
                    already_generated.add(rule.__only_rule_repr__())
                    yield rule

        if token.index_in_sentence > 1 and token.index_in_sentence + 2 < len(sentence):
            for (description, before, after) in template(2, sentence[token.index_in_sentence - 2], sentence[token.index_in_sentence - 1], token, sentence[token.index_in_sentence + 1], sentence[token.index_in_sentence + 2]):
                id_counter += 1
                rule = Rule(id_counter, description, before, after, 2, [])
                if rule.__only_rule_repr__() not in already_generated:
                    already_generated.add(rule.__only_rule_repr__())
                    yield rule

        if token.index_in_sentence > 0 and token.index_in_sentence + 3 < len(sentence):
            for (description, before, after) in template(1, sentence[token.index_in_sentence - 1], token, sentence[token.index_in_sentence + 1], sentence[token.index_in_sentence + 2], sentence[token.index_in_sentence + 3]):
                id_counter += 1
                rule = Rule(id_counter, description, before, after, 1, [])
                if rule.__only_rule_repr__() not in already_generated:
                    already_generated.add(rule.__only_rule_repr__())
                    yield rule

        if token.index_in_sentence + 4 < len(sentence):
            for (description, before, after) in template(0, token, sentence[token.index_in_sentence + 1], sentence[token.index_in_sentence + 2], sentence[token.index_in_sentence + 3], sentence[token.index_in_sentence + 4]):
                id_counter += 1
                rule = Rule(id_counter, description, before, after, 0, [])
                if rule.__only_rule_repr__() not in already_generated:
                    already_generated.add(rule.__only_rule_repr__())
                    yield rule


def full_check_rule(rule):
    if rule is None:
        return None

    rule.reset()
    for sentence in sentences:
        rule.find_errors_for(sentence)

        if rule.tp + rule.fp >= 7 and precision(rule, 0) < 0.6:
            return None

    return rule


def fast_check_rule(rule):
    for sentence, token in errors:
        rule.find_error_for_token(sentence, token)

    return rule


def fast_rule_check_generator():
    global counter
    with Pool(4) as pool:
        for rule in pool.imap_unordered(fast_check_rule, proto_rule_gen()):
            counter += 1
            if rule.tp > 2:
                yield rule


if __name__ == '__main__':
    freeze_support()

    print("Text Blocks count:", len(sentences))
    print("Errors count:", len(errors))

    start = datetime.now()

    counter = 0
    filtered_counter = 0
    last_counter = 0

    with Pool(4) as pool, open('tbl_results.json', 'w', encoding="utf-8") as file:
        file.write("[\n")
        for rule in pool.imap_unordered(full_check_rule, fast_rule_check_generator()):
            if int(counter / 10000) != int(last_counter):
                last_counter = int(counter / 10000)
                print("Rules filtered:", last_counter * 10000, "Extracted:", filtered_counter)
            if rule is not None:
                filtered_counter += 1
                record = {
                    "id": rule.id,
                    "desc": rule.description,
                    "tp": rule.tp,
                    "fp": rule.fp,
                    "errors": rule.errors_indexes
                }

                file.write(json.dumps(record, indent="  "))
                file.write(",\n")
        file.write("]")

    # for rule in rules:
    #     for error in rule.errors:
    #         new_tokens = []
    #         for token in error.tokens:
    #             new_tokens.append(sentences[token.index_in_block][token.index_in_sentence])
    #         error.tokens = new_tokens

    end = datetime.now()
    print("Rules initialized", (end - start).seconds / 60., "min")

    # calc_removed = False
    # removed = dict()
    #
    # for rule in rules:
    #     if not rule.useless:
    #         if rule.score <= 1:
    #             rule.useless = True
    #
    # survivors = set()
    # for rule in rules:
    #     if rule.useless:
    #         if calc_removed:
    #             template = []
    #             for part in rule.description['if']:
    #                 if "pos" in part:
    #                     template.append("pos")
    #                 elif "chunk" in part:
    #                     template.append("chunk")
    #                 elif "lemma" in part:
    #                     template.append("lemma")
    #                 else:
    #                     template.append("any")
    #
    #             template = tuple(template)
    #             if template not in removed:
    #                 removed[template] = 0
    #
    #             removed[tuple(template)] += 1
    #     else:
    #         rule.calc_score()
    #         survivors.add(rule)
    #
    # if calc_removed:
    #     for r in removed.items():
    #         print(r)
    #
