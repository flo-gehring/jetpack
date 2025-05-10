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
You can create Objects from the parse tree via the `@FromRule` and `@FromChild` annotations. 
There are tests documenting how to do this.  
Caveat: Grammar Stucture and Object Structure have to closely match

Classes that are parsed need to be annotated with `@FromRule`, so there needs to be one Rule per class.
Fields of the Class which need to be populated from the grammar need to be annotated with `@FromChild(index = <xy>)`. 
The index (starting at 0) is the index of the corresponding child of the rule corresponding to the class. 
See `de.flogehring.jetpack.annotationmapper.MapperTest` on how to use this.

#### Object Creation Strategies
As of now, there are two Creation Strategies, which define how an object is actually created: 
* `CreationStrategyReflection`: First creates an object with a parameterless constructor and then sets the fields via reflection.
  * Caveats: All Fields of the class need to be public and mutable. There needs to be a parameterless public constructor.
* `CreationStrategyConstructor`:  Creates the object with a given constructor.
  * Is more "hands on". You need to define which constructor to use with the `@CreatorConstructor` annotation and specify the order of the fields.
