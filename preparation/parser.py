import codecs
import sys
from importlib import reload

from nltk.corpus.reader import NOUN, ADV, VERB, ADJ
import os
from nltk_opennlp.chunkers import OpenNLPChunker
from nltk_opennlp.taggers import OpenNLPTagger
from nltk.stem import WordNetLemmatizer
import re
import nltk
from nltk import pos_tag, tree2conlltags
from rules import Token
import stanfordnlp
from stanfordnlp.pipeline.doc import Sentence, Word, Token as SToken


nltk.download('averaged_perceptron_tagger')
nltk.download('maxent_ne_chunker')
nltk.download('words')
nltk.download('wordnet')

tagger = OpenNLPTagger(language='en',
                       path_to_bin=os.path.join('apache-opennlp-1.9.2', 'bin'),
                       path_to_model=os.path.join('apache-opennlp-1.9.2/models', 'en-pos-maxent.bin'))

chunker = OpenNLPChunker(path_to_bin=os.path.join('apache-opennlp-1.9.2', 'bin'),
                         path_to_chunker=os.path.join('apache-opennlp-1.9.2/models', 'en-chunker.bin'))

# nlp = stanfordnlp.Pipeline()
#
#
# with codecs.open("typos/'M_PREP'.type", encoding="utf-8") as errors, open('sentences_beta.data', 'w', encoding="utf-8") as data:
#     while True:
#         block = errors.readline()
#         if block.strip() == '':
#             break
#
#         doc = nlp(block)
#         for sentence in doc.sentences:
#             sentence: Sentence
#             representation = []
#             chunks = list(map(lambda info: info[2], tree2conlltags(chunker.parse(tagger.tag(sentence)))))
#             for word in sentence.words:
#                 word: Word
#
#                 if word.index == len(sentence.words):
#                     if chunks[word.index - 1].startswith("B-"):
#                         chunks[word.index - 1] = chunks[word.index - 1][2:]
#                     elif chunks[word.index - 1].startswith("I-"):
#                         chunks[word.index - 1] = "E" + chunks[word.index - 1][1:]
#                 else:
#                     if chunks[word.index - 1].startswith("B-") and not chunks[word.index].startswith("I-"):
#                         chunks[word.index - 1] = chunks[word.index - 1][2:]
#                     elif chunks[word.index - 1].startswith("I-") and not chunks[word.index].startswith("I-"):
#                         chunks[word.index - 1] = "E" + chunks[word.index - 1][1:]
#
#                 representation.append("(" +
#                                       str(word.index) + " " +
#                                       str(word.text) + " " +
#                                       str(word.lemma) + " " +
#                                       str(word.pos) + " " +
#                                       chunks[word.index - 1] + " " +
#                                       str("START" if word.index == 1 else "END" if word.index == len(sentence.words) else "MID") + " " +
#                                       str(word.governor) + " " +
#                                       str(word.dependency_relation) + " " +
#                                       ")")
#             data.write("#".join(representation))
#             data.write('\n')
#
#         while True:
#             error = errors.readline()
#             if error.strip() == '':
#                 break


lemmetaizer = WordNetLemmatizer()


def lemmatize(text, pos):
    lpos = ""
    if pos in ["NNS", "NN", "NNP", "NNPS"]:
        lpos = NOUN
    elif pos in ["RBR", "RB", "RBS"]:
        lpos = ADV
    elif pos in ["VB", "VBG", "VBD", "VBN", "VBP", "VBZ"]:
        lpos = VERB
    elif pos in ["JJ", "JJR", "JJS"]:
        lpos = ADJ

    if lpos != "":
        return lemmetaizer.lemmatize(text.lower(), lpos)
    else:
        return text.lower()


counter = 0
with codecs.open("typos_dev_a/'R_PREP'.type", encoding="utf-8") as errors, open('sentences_beta_dev_a.data', 'w', encoding="utf-8") as data:
    while True:
        sentence = errors.readline()
        if sentence.strip() == '':
            break

        counter += 1
        print(counter)

        raw_tokens = list(map(lambda t: Token(t[0], lemmatize(t[0], t[1]), t[1], t[2]), tree2conlltags(chunker.parse(tagger.tag(sentence)))))

        while True:
            error = errors.readline()
            if error.strip() == '':
                break

            match = re.match("^Orig: \\[([0-9]+), ([0-9]+), \'([^']*)\'\\], Cor: \\[[0-9]+, [0-9]+, \'([^']*)\'\\]", error)
            if match:
                raw_tokens[int(match.group(1))].correction = match.group(4)
            # if match and match.group(3) == '' and match.group(4) == ',':
            #     if match.group(1) != match.group(2):
            #         raise Exception("Error position not match")
            #
            #     raw_tokens[int(match.group(1))].comma_needed = True

        tokens = []
        is_comma_needed = False
        prev = None
        for token in raw_tokens:
            if prev is not None:
                if prev.chunk.startswith("B-") and not token.chunk.startswith("I-"):
                    prev.chunk = prev.chunk[2:]
                elif prev.chunk.startswith("I-") and not token.chunk.startswith("I-"):
                    prev.chunk = "E" + prev.chunk[1:]

            token.index_in_sentence = len(tokens)
            tokens.append(token)

            prev = token

        data.write("#".join(map(lambda x: str(x), tokens)))
        data.write('\n')
        data.flush()
