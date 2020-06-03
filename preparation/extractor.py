import codecs
import copy
import json
from typing import List

import errant
import nltk
from errant.edit import Edit

annotator = errant.load('en')

counter = 0
error_types_files = dict()
buffer = []
with open("data/Train.json") as data, open("correct.type", "w", encoding="utf") as correct_file:
    for line in data:
        counter += 1
        print(counter)

        block = json.loads(line)
        edits = sorted(block['edits'][0][1], key=lambda edit: edit[0], reverse=True)

        original: str = block['text']
        original_tokenized_list = nltk.word_tokenize(original.replace('*', ''))
        original_tokenized = " ".join(original_tokenized_list)

        corrected = original
        for edit in edits:
            corrected = corrected[:edit[0]] + (edit[2] if edit[2] is not None else "") + corrected[edit[1]:]
        corrected_tokenized = " ".join(nltk.word_tokenize(corrected.replace('*', '')))

        correct_file.write(corrected_tokenized)
        correct_file.write("\n")

        errant_edits: List[Edit] = list(map(lambda e: e, annotator.annotate(annotator.parse(original_tokenized), annotator.parse(corrected_tokenized))))

        for edit in errant_edits:
            print(edit)
        print()
        errant_edits = errant_edits[::-1]

        block_error_types = dict()
        for edit in errant_edits:
            type = repr(edit.type)

            if type not in error_types_files:
                error_types_files[type] = codecs.open("typos/" + str(type).replace(':', '_') + ".type", "w", encoding="utf-8")
                for block in buffer:
                    tokens_string = " ".join(block)
                    error_types_files[type].write(tokens_string)
                    error_types_files[type].write('\n\n')

            if type not in block_error_types:
                block_error_types[type] = copy.copy(original_tokenized_list)

            for stype, tokens in block_error_types.items():
                if stype != type:
                    block_error_types[stype] = tokens[:edit.o_start] + ([] if edit.c_str == '' else [edit.c_str]) + tokens[edit.o_end:]

            original_tokenized_list = original_tokenized_list[:edit.o_start] + ([] if edit.c_str == '' else [edit.c_str]) + original_tokenized_list[edit.o_end:]

        buffer.append(copy.copy(original_tokenized_list))
        for type, file in error_types_files.items():
            if type not in block_error_types:
                block_error_types[type] = copy.copy(original_tokenized_list)

        for type, tokens in block_error_types.items():
            tokens_string = " ".join(tokens)
            error_types_files[type].write(tokens_string)
            error_types_files[type].write('\n')

            for edit in annotator.annotate(annotator.parse(tokens_string), annotator.parse(corrected_tokenized)):
                if repr(edit.type) != type:
                    print("WRONG:", edit, type)
                    edit.type = type[1:-1]

                error_types_files[type].write(str(edit))
                error_types_files[type].write('\n')

            error_types_files[type].write('\n')
