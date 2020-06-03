from typing import Pattern
from rules import Rule, Token, Error
from typing import List, Optional, Dict


class RuleCondition:
    def __init__(self, field: str, value, negation: bool):
        self.field = field
        self.value = value
        self.negation = negation

        self.pos = field == "pos"
        self.chunk = field == "chunk"
        self.lemma = field == "lemma"

        self.remove_chunk_prefix = False

        if self.chunk:
            if "-" not in value:
                self.remove_chunk_prefix = True


class RuleToken:
    def __init__(self, selector: Dict):
        relative_position = int(selector["__position"])
        self.position = (relative_position if selector["__name"] != "previous" else -relative_position)
        self.conditions: List[RuleCondition] = []

        for key in selector:
            if key.startswith("__"):
                continue

            negation = not selector[key][0]
            value = selector[key][1]

            self.conditions.append(RuleCondition(key, value, negation))


class Binder:
    def __init__(self, rule: Dict):
        self.rule = rule
        self.then = self.rule["then"]

        self.rule_tokens: List[RuleToken] = []

        for selector in rule["if"]:
            self.rule_tokens.append(RuleToken(selector))

    def is_appliable(self, sentence: List, token_index: int) -> bool:
        for rule_token in self.rule_tokens:
            position = token_index + rule_token.position
            if position < 0 or position >= len(sentence):
                return False

            for condition in rule_token.conditions:
                # real = sentence[position][condition.field]

                if condition.pos:
                    real = sentence[position].pos
                elif condition.chunk:
                    real = sentence[position].chunk
                    if condition.remove_chunk_prefix and "-" in real:
                        _, real = real.split("-", 1)
                elif condition.lemma:
                    real = sentence[position].lemma
                else:
                    continue

                if condition.negation:
                    if isinstance(condition.value, Pattern):
                        if len(condition.value.findall(real)) > 0:
                            return False
                    elif real == condition.value:
                        return False
                else:
                    if isinstance(condition.value, Pattern):
                        if len(condition.value.findall(real)) == 0:
                            return False
                    elif real != condition.value:
                        return False

        return True

    def check_token(self, sentence: List[Token], token: Token, info: Rule):
        if self.is_appliable(sentence, token.index_in_sentence):
            tokens = sentence[token.index_in_sentence - info.tokens_before: token.index_in_sentence + info.tokens_after + 1]
            return Error(tokens, info.tokens_at, self.then)

    def check_sentence(self, sentence: List[Token], info: Rule):
        errors = []
        for token in sentence:
            if self.is_appliable(sentence, token.index_in_sentence):
                tokens = sentence[token.index_in_sentence - info.tokens_before : token.index_in_sentence + info.tokens_after + 1]
                errors.append(Error(tokens, info.tokens_at, self.then))
                # errors.append(Error([token], 0, self.then))

        return errors
