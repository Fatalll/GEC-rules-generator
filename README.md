# GEC-rules-generator

GEC rules generator is a utility for extracting rules from the marked-up corpus of text for checking and correcting errors.

* [datasets](https://github.com/Fatalll/GEC-rules-generator/tree/master/src/main/resources/datasets) directory contains datasets with the English text corpus containing errors with their corrections.
* [data](https://github.com/Fatalll/GEC-rules-generator/tree/master/src/main/resources/data/train) directory contains pre-processed data with [ERRANT](https://github.com/chrisjbryant/errant) from the datasets above, sorted into categories of grammatical errors.
* [results](https://github.com/Fatalll/GEC-rules-generator/tree/master/data/results) directory contains generated rules for this datasets.

Representation of the rules contained in [rules](https://github.com/Fatalll/GEC-rules-generator/tree/master/src/main/kotlin/rules) directory.
A rule consists of a detector and an editor. A detector consists of a set of [conditions](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/rules/Condition.kt) that must be met.
The editor consists of a set of [modifications](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/rules/Modifications.kt) that must be applied to the sentence.
A rule can be [serialized](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/rules/Moshi.kt) in JSON and vice versa.

For rule generation you must сompress the source data into a [graph](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/generation/Graphs.kt).
After that pass blocks of text with errors and their corrections to [generate()](https://github.com/Fatalll/GEC-rules-generator/blob/34d7cf57e5aa5886687b1bbe68630de8b4c9ce72/src/main/kotlin/generation/Generation.kt#L254) method.
To work with datasets to retrieve features, you can use the [NLP](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/extraction/NLP.kt) class.

An example for rules generation of the English text corpus can be found in [Main.kt](https://github.com/Fatalll/GEC-rules-generator/blob/master/src/main/kotlin/Main.kt)
