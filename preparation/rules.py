from math import floor
from typing import List, Optional, Dict


def precision(rule, tolerance=5):
    if rule.fp + rule.tp == 0:
        return 0

    flag = rule.fp <= tolerance
    fp = 0 if flag else rule.fp
    tp = (0 if rule.tp <= rule.fp else rule.tp - rule.fp) if flag else rule.tp

    pr = 0 if fp + tp == 0 else tp / (tp + fp)
    return round(pr, 2)


class Token(dict):
    def __init__(self, text: str, lemma: str, pos: str, chunk: str,
                 comma_before: bool = False, comma_needed: bool = False,
                 index_in_sentence: int = 0, index_in_block: int = 0):
        super().__init__()
        self.__dict__ = self
        self.text = text
        self.lemma = lemma
        self.pos = pos
        self.chunk = chunk
        self.comma_before = comma_before
        self.comma_needed = comma_needed
        self.index_in_sentence = index_in_sentence
        self.index_in_block = index_in_block
        self.correction = ""

        self.used = False

    def copy(self):
        return Token(self.text, self.lemma, self.pos, self.chunk,
                     self.comma_before, self.comma_needed,
                     self.index_in_sentence, self.index_in_block)

    def __str__(self):
        return "('" + self.text + "', " + self.lemma + ", " + self.pos + ", " + self.chunk + ")"

    def __repr__(self):
        return str(self)

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Token):
            return False

        return self.index_in_sentence == other.index_in_sentence and self.index_in_block == other.index_in_block

    def __ne__(self, other: object) -> bool:
        return not (self == other)

    def __hash__(self):
        return hash((self.index_in_sentence, self.index_in_block))


class Error:
    def __init__(self, tokens: List[Token], index: int, fix: Dict):
        self.tokens = tokens
        self.index = index
        self.fix = fix

    def error_token(self):
        return self.tokens[self.index]

    def is_fix_correct(self):
        for key, value in self.fix.items():
            if key == "comma_before":
                if self.error_token()["comma_needed"] != value:
                    return False
            if key == "lemma":
                if self.error_token().correction != value and self.error_token().lemma != value:
                    return False
            # if key == "text":
            #     if self.error_token().text != value:
            #         return False

        return True


class Rule:
    def __init__(self, id: int, description: Dict, tokens_before: [int], tokens_after: [int], token_at: [int], errors: List[Error],
                 tp: int = 0, fp: int = 0, score: int = 0):
        self.id = id
        self.description = description
        from binder import Binder
        self.parser = Binder(description)
        self.score = score
        self.tp = tp
        self.fp = fp
        self.errors = errors
        self.errors_indexes = []
        self.tokens_before = tokens_before
        self.tokens_after = tokens_after
        self.tokens_at = token_at
        self.useless = False

    def reset(self):
        self.tp = 0
        self.fp = 0
        self.score = 0
        self.errors = []
        self.errors_indexes = []
        self.useless = False

    def update_score_for_error(self, error: Error):
        token = error.error_token()
        for key, value in error.fix.items():
            if key == "lemma":
                if token.correction == value or token.lemma == value:
                    self.score += 1
                    self.tp += 1
                else:
                    self.fp += 1

            elif key == "comma_before":
                if token["comma_needed"] == value:
                    self.score += 1
                    self.tp += 1
                else:
                    # self.score -= 1
                    self.fp += 1

    def find_errors_for(self, sentence: List[Token], expose_errors: bool = False):
        new_errors = self.parser.check_sentence(sentence, self)

        for error in new_errors:
            self.update_score_for_error(error)
            self.errors_indexes.append((error.error_token().index_in_block, error.error_token().index_in_sentence))

        if expose_errors:
            self.errors.extend(new_errors)

    def find_error_for_token(self, sentence: List[Token], token: Token, expose_errors: bool = False):
        error = self.parser.check_token(sentence, token, self)

        if error:
            self.update_score_for_error(error)
            self.errors_indexes.append((error.error_token().index_in_block, error.error_token().index_in_sentence))

            if expose_errors:
                self.errors.append(error)

    def calc_score(self):
        self.score = 0
        self.tp = 0
        self.fp = 0
        for error in self.errors:
            self.update_score_for_error(error)

    def apply(self):
        for error in self.errors:
            token = error.error_token()

            for key, value in error.fix.items():
                if key == "comma_before" and token[key] != value:
                    # apply only correct ones
                    # if token["comma_needed"] == value:
                    token[key] = value
                if key != "any" and key != "comma_before":
                    if token[key][0].isupper():
                        token[key] = value.capitalize()
                    else:
                        token[key] = value

    def print_errors(self):
        for error in self.errors:
            print(error.tokens, "->", error.fix)

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Rule):
            return False

        return self.description == other.description

    def __ne__(self, other: object) -> bool:
        return not (self == other)

    def __hash__(self) -> int:
        return hash(self.__only_rule_repr__())

    def __only_rule_repr__(self) -> str:
        rep = []
        for token_rule in self.description['if']:
            conds = []
            if "chunk" in token_rule:
                conds.append("c: " + token_rule['chunk'][1])
            if "pos" in token_rule:
                conds.append("p: " + token_rule['pos'][1])
            if "lemma" in token_rule:
                conds.append("l: " + token_rule['lemma'][1])
            if "any" in token_rule:
                conds.append("any")
            # else:
            #     raise Exception("unsupported type while parsing tbl results")

            token = ", ".join(conds)

            if token_rule['__name'] == "next" and token_rule['__position'] == 0:
                token = "[" + token + "]"
            else:
                token = "(" + token + ")"

            rep.append(token)

        then = ", ".join([str(v) for v in self.description['then'].items()])

        return " ".join(rep) + " -> " + then

    def __repr__(self) -> str:
        return "ID: " + str(self.id) + ", TP: " + str(self.tp) + ", FP: " + str(self.fp) + ", rule: " + self.__only_rule_repr__()
