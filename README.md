# 🚀JetpackParser

> [!WARNING]  
> It probably is obvious but this just a hobby project, and should not be used in any serious capacity.  
> There are not many tests but surely a lot of bugs.  
> Most features of a mature parser are missing.
## Scope 
This implements a PEG Parser with the additional Feature, that it can parse left recursive grammars. To do so, it follows
the approach of [Umeda and Maeda: Packrat Parsers Can Support Multiple Left-recursive
Calls at the Same Position](https://www.jstage.jst.go.jp/article/ipsjjip/29/0/29_174/_pdf).

Furthermore, it provides a way to construct Java-Objects from the parse tree. You can use the resolver function builder
or an annotation based approach. There are test cases as an example for both approaches.

### Annotation based Object Construction
You can create Objects from the parse tree vie the `@FromRule` and `@FromChild` annotations. 
There are tests documenting how to do this.  
Caveats:
* Grammar Stucture and Object Structure have to closely match
* The parsing of "primitive" type is limited to the native Java `fromString` Methods
* The fields of the need to be public and not final.
* A parameterless constructor for each class that is constructed by `@FromRule` must exist. 
* :warning: Do expect the annotations to change.
