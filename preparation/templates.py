from itertools import product

BAD_TEMPLATES = {
    # ('lemma', 'chunk', 'lemma', 'lemma', 'pos'),
    # ('lemma', 'chunk', 'lemma', 'lemma', 'lemma'),
    # ('lemma', 'any', 'lemma', 'lemma', 'lemma'),
    # ('lemma', 'pos', 'lemma', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'pos', 'pos', 'lemma'),
    # ('lemma', 'lemma', 'pos', 'lemma', 'pos'),
    # ('lemma', 'lemma', 'pos', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'pos', 'chunk', 'lemma'),
    # ('lemma', 'lemma', 'pos', 'any', 'lemma'),
    # ('lemma', 'lemma', 'lemma', 'pos', 'lemma'),
    # ('lemma', 'lemma', 'lemma', 'lemma', 'pos'),
    # ('lemma', 'lemma', 'lemma', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'lemma', 'chunk', 'lemma'),
    # ('lemma', 'lemma', 'lemma', 'any', 'lemma'),
    # ('lemma', 'lemma', 'chunk', 'pos', 'lemma'),
    # ('lemma', 'lemma', 'chunk', 'lemma', 'pos'),
    # ('lemma', 'lemma', 'chunk', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'any', 'pos', 'lemma'),
    # ('lemma', 'lemma', 'any', 'lemma', 'pos'),
    # ('lemma', 'lemma', 'any', 'lemma', 'lemma'),
    # ('lemma', 'chunk', 'pos', 'lemma', 'pos'),
    # ('lemma', 'chunk', 'lemma', 'lemma', 'chunk'),
    # ('lemma', 'chunk', 'lemma', 'lemma', 'any'),
    # ('lemma', 'chunk', 'lemma', 'chunk', 'lemma'),
    # ('lemma', 'chunk', 'lemma', 'any', 'lemma'),
    # ('lemma', 'any', 'lemma', 'lemma', 'pos'),
    # ('lemma', 'pos', 'lemma', 'pos', 'lemma'),
    # ('lemma', 'pos', 'lemma', 'lemma', 'pos'),
    # ('lemma', 'pos', 'any', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'pos', 'lemma', 'chunk'),
    # ('lemma', 'lemma', 'pos', 'lemma', 'any'),
    # ('lemma', 'lemma', 'lemma', 'lemma', 'chunk'),
    # ('lemma', 'lemma', 'lemma', 'lemma', 'any'),
    # ('lemma', 'lemma', 'chunk', 'lemma', 'chunk'),
    # ('lemma', 'lemma', 'chunk', 'chunk', 'lemma'),
    # ('lemma', 'lemma', 'chunk', 'any', 'lemma'),
    # ('lemma', 'lemma', 'any', 'chunk', 'lemma'),
    # ('lemma', 'lemma', 'any', 'any', 'lemma'),
    # ('lemma', 'chunk', 'pos', 'lemma', 'lemma'),
    # ('lemma', 'chunk', 'pos', 'lemma', 'chunk'),
    # ('lemma', 'chunk', 'lemma', 'pos', 'lemma'),
    # ('lemma', 'chunk', 'chunk', 'lemma', 'lemma'),
    # ('lemma', 'chunk', 'any', 'lemma', 'lemma'),
    # ('lemma', 'any', 'pos', 'lemma', 'lemma'),
    # ('lemma', 'any', 'chunk', 'lemma', 'lemma'),
    # ('lemma', 'any', 'any', 'lemma', 'lemma'),
    # ('pos', 'lemma', 'lemma', 'lemma', 'lemma'),
    # ('lemma', 'pos', 'pos', 'lemma', 'lemma'),
    # ('lemma', 'pos', 'lemma', 'lemma', 'chunk'),
    # ('lemma', 'pos', 'chunk', 'lemma', 'lemma'),
    # ('lemma', 'lemma', 'any', 'lemma', 'chunk'),
    # ('lemma', 'chunk', 'pos', 'chunk', 'lemma'),
    # ('lemma', 'chunk', 'any', 'pos', 'lemma'),
    # ('lemma', 'any', 'lemma', 'pos', 'lemma'),
    # ('lemma', 'any', 'lemma', 'lemma', 'chunk'),
    # ('lemma', 'any', 'lemma', 'chunk', 'lemma'),
    # ('lemma', 'any', 'lemma', 'any', 'lemma'),
    # ('chunk', 'lemma', 'pos', 'lemma', 'lemma'),
    # ('chunk', 'lemma', 'lemma', 'pos', 'lemma'),
    # ('chunk', 'lemma', 'lemma', 'lemma', 'lemma'),
    # ('chunk', 'lemma', 'lemma', 'chunk', 'lemma'),
    # ('chunk', 'lemma', 'lemma', 'any', 'lemma'),
    # ('chunk', 'lemma', 'chunk', 'lemma', 'lemma'),
    # ('chunk', 'lemma', 'any', 'lemma', 'lemma'),
    # ('pos', 'lemma', 'pos', 'lemma', 'lemma'),
    # ('pos', 'lemma', 'lemma', 'pos', 'lemma'),
    # ('pos', 'lemma', 'lemma', 'chunk', 'lemma'),
    # ('pos', 'lemma', 'lemma', 'any', 'lemma'),
    # ('pos', 'lemma', 'chunk', 'lemma', 'lemma'),
    # ('pos', 'lemma', 'any', 'lemma', 'lemma'),
    # ('lemma', 'pos', 'lemma', 'lemma', 'any'),
    # ('lemma', 'pos', 'lemma', 'chunk', 'lemma'),
    # ('lemma', 'pos', 'lemma', 'any', 'lemma'),
    # ('lemma', 'lemma', 'lemma', 'pos', 'pos'),
    # ('lemma', 'lemma', 'lemma', 'chunk', 'pos'),
    # ('lemma', 'lemma', 'lemma', 'any', 'pos'),
    # ('lemma', 'lemma', 'chunk', 'lemma', 'any'),
    # ('lemma', 'chunk', 'pos', 'pos', 'lemma'),
    # ('lemma', 'chunk', 'pos', 'any', 'lemma'),
    # ('lemma', 'chunk', 'chunk', 'pos', 'lemma'),
    # ('lemma', 'chunk', 'chunk', 'lemma', 'pos'),
    # ('lemma', 'chunk', 'chunk', 'chunk', 'lemma'),
    # ('lemma', 'any', 'lemma', 'lemma', 'any'),
    ('any', 'any', 'any', 'any', 'any'),

    ('lemma', 'chunk', 'lemma', 'lemma'),
    ('lemma', 'lemma', 'chunk', 'lemma'),
    ('lemma', 'lemma', 'lemma', 'lemma'),
    ('lemma', 'lemma', 'pos', 'lemma'),
    ('lemma', 'pos', 'lemma', 'lemma'),
    ('lemma', 'lemma', 'lemma', 'chunk'),
    ('lemma', 'lemma', 'lemma', 'pos'),
    ('lemma', 'pos', 'pos', 'lemma'),
    ('lemma', 'lemma', 'pos', 'pos'),
    ('pos', 'lemma', 'lemma', 'lemma'),
    ('lemma', 'chunk', 'pos', 'lemma'),
    ('chunk', 'lemma', 'lemma', 'lemma'),
    ('lemma', 'lemma', 'pos', 'chunk'),
    ('lemma', 'lemma', 'chunk', 'pos'),
    ('lemma', 'pos', 'chunk', 'lemma'),
    ('pos', 'lemma', 'chunk', 'lemma'),
    ('lemma', 'chunk', 'chunk', 'lemma'),
    ('pos', 'lemma', 'pos', 'lemma'),
    ('lemma', 'pos', 'lemma', 'pos'),
    ('pos', 'lemma', 'lemma', 'pos'),
    ('lemma', 'lemma', 'chunk', 'chunk'),
    ('lemma', 'chunk', 'lemma', 'pos'),
    ('chunk', 'lemma', 'pos', 'lemma'),
    ('pos', 'lemma', 'lemma', 'chunk'),
    ('pos', 'pos', 'lemma', 'lemma'),
    ('chunk', 'lemma', 'chunk', 'lemma'),
    ('lemma', 'pos', 'lemma', 'chunk'),
    ('chunk', 'lemma', 'lemma', 'pos'),
    ('lemma', 'pos', 'pos', 'pos'),
    ('pos', 'chunk', 'lemma', 'lemma'),
    ('lemma', 'chunk', 'lemma', 'chunk'),
    ('lemma', 'chunk', 'pos', 'pos'),
    ('chunk', 'pos', 'lemma', 'lemma'),
    ('chunk', 'lemma', 'lemma', 'chunk'),
    ('lemma', 'pos', 'chunk', 'pos'),

    ('lemma', 'lemma', 'lemma'),
    ('lemma', 'lemma', 'pos'),
    ('lemma', 'pos', 'lemma'),
    ('lemma', 'lemma', 'chunk'),
    ('lemma', 'chunk', 'lemma')
}


def generator(num):
    for template in product(["pos", "chunk", "lemma", "any"], repeat=num):
        if tuple(template) not in BAD_TEMPLATES:
            yield template


def template(err_ind, *tokens):
    before = err_ind
    after = len(tokens) - 1 - err_ind
    for vals in generator(len(tokens)):
        rule = {
                  "if": [
                      {
                          "__name": "previous" if err_ind > ind else "next",
                          "__position": err_ind - ind if err_ind > ind else ind - err_ind,
                          vals[ind]: (True, True if vals[ind] not in token else token[vals[ind]])
                          # "comma_before": (True, False)
                      } for ind, token in enumerate(tokens)
                  ],
                  "then": {
                      "lemma": tokens[err_ind].correction,
                      "text": tokens[err_ind].correction
                  }
              }

        rule['if'][err_ind]["lemma"] = (True, tokens[err_ind].lemma)
        yield rule, before, after


def double_tag_template(prev, curr):
    return {
        "if": [
            {
                "__name": "previous",
                "__position": 1,
                "pos": [True, prev],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 0,
                "pos": [True, curr],
                "comma_before": [True, False]
            }
        ],
        "then": {
            "comma_before": True
        }
    }


def triple_tag_template(prev, curr, next):
    return {
        "if": [
            {
                "__name": "previous",
                "__position": 1,
                "pos": [True, prev],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 0,
                "pos": [True, curr],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 1,
                "pos": [True, next],
                "comma_before": [True, False]
            }
        ],
        "then": {
            "comma_before": True
        }
    }


def triple_tag_template_with_word(prev, curr_word, next):
    return {
        "if": [
            {
                "__name": "previous",
                "__position": 1,
                "chunk": [True, prev],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 0,
                "text": [True, curr_word],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 1,
                "chunk": [True, next],
                "comma_before": [True, False]
            }
        ],
        "then": {
            "comma_before": True
        }
    }


def fourth_tag_template(prev_prev, prev, curr, next):
    return {
        "if": [
            {
                "__name": "previous",
                "__position": 2,
                "pos": [True, prev_prev],
                "comma_before": [True, False]
            },
            {
                "__name": "previous",
                "__position": 1,
                "pos": [True, prev],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 0,
                "pos": [True, curr],
                "comma_before": [True, False]
            },
            {
                "__name": "next",
                "__position": 1,
                "pos": [True, next],
                "comma_before": [True, False]
            }
        ],
        "then": {
            "comma_before": True
        }
    }
