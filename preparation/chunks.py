import os

from nltk import tree2conlltags
from nltk_opennlp.chunkers import OpenNLPChunker
from nltk_opennlp.taggers import OpenNLPTagger
from pattern.text.en import parse

tagger = OpenNLPTagger(language='en',
                       path_to_bin=os.path.join('apache-opennlp-1.9.2', 'bin'),
                       path_to_model=os.path.join('apache-opennlp-1.9.2/models',
                                                  'en-pos-maxent.bin'))

chunker = OpenNLPChunker(
    path_to_bin=os.path.join('apache-opennlp-1.9.2', 'bin'),
    path_to_chunker=os.path.join('apache-opennlp-1.9.2/models',
                                 'en-chunker.bin'))

with open("correct.type", encoding="utf-8") as data:
    good = 0
    bad = 0
    other = 0

    counter = 0
    for line in data:
        counter += 1

        lol = []
        onlp = []
        for x in parse(line, tokenize=False, tags=False, chunks=True, relations=False,
                       lemmata=False, encoding='utf-8', tagset=None).split()[0]:
            #print(x[0] + "/" + x[1] + "/" + x[2], end=" ")
            lol.append(x[2])

        for x in tree2conlltags(chunker.parse(tagger.tag(line))):
            #print(x[0] + "/" + x[1] + "/" + x[2], end=" ")
            onlp.append(x[2])

        for l, o in zip(lol, onlp):
            if l == o:
                good += 1
            else:
                if "NP" in o or "PP" in o or "VP" in o or "ADVP" in o or "ADJP" in o:
                    if l != "O":
                        bad += 1
                    else:
                        other += 1

                else:
                    other += 1

        print(counter, good, bad, other)
