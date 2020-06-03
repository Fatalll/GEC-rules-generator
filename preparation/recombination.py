import itertools
import json
import re
from typing import Dict, List, Set, Tuple

from rules import Token, Rule, Error, precision


def load_sentences(path: str = "sentences.data"):
    sentences = []
    with open(path, encoding="utf-8") as errors:
        for line in errors:
            tokens = []
            for error in line.split('#'):
                match = re.match("\\(\'(.+)\', (.+), ([A-Z.,\\-'`№$:]+), ([№A-Z\\-]+), cb: ([A-Za-z]*), n: ([A-Za-z]*)\\)", error)
                if match is None:
                    print(error)
                token = Token(match.group(1), match.group(2), match.group(3), match.group(4), match.group(5) == "True", match.group(6) == "True")
                # token.correction = match.group(5).lower()
                token.index_in_sentence = len(tokens)
                token.index_in_block = len(sentences)
                tokens.append(token)

            sentences.append(tokens)
    return sentences


def load_rules(sentences: List[List[Token]]):
    rules = dict()
    with open("rules/M_PUNCT/tbl_results.json", encoding="utf-8") as file:
        data = json.load(file)

        for rule in data:
            err_ind = 0
            for selector in rule['desc']['if']:
                if int(selector['__position']) == 0:
                    break
                err_ind += 1

            before = err_ind
            after = 5 - 1 - err_ind

            errors = []
            for error in rule['errors']:
                token = sentences[error[0]][error[1]]
                tokens = sentences[error[0]][
                         token.index_in_sentence - before: token.index_in_sentence + after + 1]
                errors.append(Error(tokens, err_ind, rule['desc']['then']))

            rules[rule['id']] = Rule(rule['id'], rule['desc'], before, after,
                                     err_ind, errors, rule['tp'], rule['fp'])
    return rules


class ErrorInfo:
    def __init__(self, token: Token) -> None:
        self.token = token
        self.errors = set()
        self.rules = set()

    def __repr__(self) -> str:
        return str(self.token)


sentences = load_sentences("sentences.data")
rules = load_rules(sentences)
errors: Dict[Token, ErrorInfo] = dict()
set_of_rules: Set[Rule] = set()
counter = 0
for rule in rules.values():
    if precision(rule, 0) > 0.92 and 2 < rule.tp:
        set_of_rules.add(rule)
        counter += 1
        for err in rule.errors:
            if err.is_fix_correct():
                token = err.error_token()
                if token not in errors:
                    errors[token] = ErrorInfo(token)

                errors[token].rules.add(rule)
                errors[token].errors.add(err)

print(counter)
# clusters = []
# while errors:
#     token, info = errors.popitem()
#     cluster = [info]
#
#     rule_cluster = info.rules.copy()
#     while rule_cluster:
#         rule = rule_cluster.pop()
#         for err in rule.errors:
#             if err.is_fix_correct():
#                 token = err.error_token()
#
#                 if token in errors:
#                     error_info = errors.pop(token)
#                     for rule in error_info.rules:
#                         rule_cluster.add(rule)
#                     cluster.append(error_info)
#     clusters.append(cluster)

clusters = []
while set_of_rules:
    rule = max(set_of_rules, key=lambda rule: (precision(rule, 0), rule.tp, -rule.fp))
    set_of_rules.remove(rule)
    cluster = [rule]
    stats: Dict[Rule, int] = dict()
    for err in rule.errors:
        if err.error_token() in errors:
            info = errors[err.error_token()]
            for r in info.rules:
                if r not in stats:
                    stats[r] = 0

                stats[r] += 1
    # min = len(rule.errors) * 0.2
    for candidate_rule, stat in stats.items():
        min_errs = min(rule.tp, candidate_rule.tp)
        max_errs = max(rule.tp, candidate_rule.tp)
        val = int(min_errs * (1 - (min_errs * 0.6) / float(max_errs)))

        if stat >= val:
            cluster.append(candidate_rule)

            if candidate_rule in set_of_rules:
                set_of_rules.remove(candidate_rule)
    clusters.append(cluster)

print(len(clusters))

# with open("clusters.data", "w", encoding="utf-8") as data:
#     for cluster in clusters:
#         for rule in cluster:
#             data.write(repr(rule))
#             data.write("\n")
#         data.write("\n")


class RuleGroup:
    def __init__(self) -> None:
        self.rules: List[Tuple[Rule, int, int]] = []
        self.tp_tokens: Set[Token] = set()
        self.fp_tokens: Set[Token] = set()

    def reset(self):
        self.tp_tokens.clear()
        self.fp_tokens.clear()
        for rule, _, _ in group.rules:
            rule.reset()

    def calc_results(self):
        for rule, _, _ in self.rules:
            for err in rule.errors:
                if err.is_fix_correct():
                    self.tp_tokens.add(err.error_token())
                else:
                    self.fp_tokens.add(err.error_token())

    def check_sentence(self, sentence: List[Token]):
        for rule, _, _ in self.rules:
            rule.find_errors_for(sentence, True)


groups: List[RuleGroup] = []
for cluster in clusters:
    cluster = sorted(cluster, key=lambda rule: (-rule.fp, rule.tp), reverse=True)
    group = RuleGroup()

    for rule in cluster:
        rule_tp_tokens, rule_fp_tokens = [], []
        for err in rule.errors:
            if not err.error_token().used:
                if err.is_fix_correct():
                    if err.error_token() not in group.tp_tokens:
                        rule_tp_tokens.append(err.error_token())
                else:
                    if err.error_token() not in group.fp_tokens:
                        rule_fp_tokens.append(err.error_token())
        if (len(rule_tp_tokens) - len(rule_fp_tokens) >= 2) and len(rule_fp_tokens) < 3:
            group.rules.append((rule, len(rule_tp_tokens), len(rule_fp_tokens)))
            for tp_token in rule_tp_tokens:
                group.tp_tokens.add(tp_token)
                tp_token.used = True
            for fp_token in rule_fp_tokens:
                group.fp_tokens.add(fp_token)
                fp_token.used = True

    if group.rules:
        if len(group.tp_tokens) > 1:
            groups.append(group)
        else:
            for tp_token in group.tp_tokens:
                tp_token.used = False
            for fp_token in group.fp_tokens:
                fp_token.used = False


for group in groups:
    group.reset()

validation_sentences = load_sentences("sentences_dev_a.data")
print("Start validation")
for sentence in validation_sentences:
    for group in groups:
        group.check_sentence(sentence)

for group in groups:
    group.calc_results()

tps, fps = 0, 0
real_errors = 0
print(len(groups))
with open("tmp_rule_groups.data", "w", encoding="utf-8") as data:
    for group in groups:
        tps += len(group.tp_tokens)
        fps += len(group.fp_tokens)

        for token in group.tp_tokens:
            real_errors += 1 if not token.comma_before and token.comma_needed else 0
            # real_errors += 1 if token.correction != "" else 0

        print("TP:", len(group.tp_tokens), "FP:", len(group.fp_tokens))
        data.write("TP: " + str(len(group.tp_tokens)) + " FP: " + str(len(group.fp_tokens)) + "\n")
        for rule, tp, fp in group.rules:
            print(rule, tp, fp)
            # for err in rule.errors:
            #     print(err.tokens)
            #     break
            data.write(repr(rule))
            data.write(", local TP: " + str(tp) + ", local FP: " + str(fp) + "\n")
        print()
        data.write("\n")

print(tps, fps, real_errors)

with open('comma_results.json', 'w', encoding="utf-8") as file:
    file.write("[\n")

    for group in groups:
        for rule, tp, fp in group.rules:
            record = {
                "id": rule.id,
                "desc": rule.description,
                "tp": rule.tp,
                "fp": rule.fp
            }

            file.write(json.dumps(record, indent="  "))
            file.write(",\n")
    file.write("]")
