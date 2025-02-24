package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.util.Check;

public sealed interface Operator extends Expression {

    record Sequence(Expression first, Expression second) implements Operator {

    }

    record OrderedChoice(Expression either, Expression or) implements Operator {

    }

    record Star(Expression exp) implements Operator {

    }

    record Optional(Expression exp) implements Operator {

        public Optional {
            Check.requireNotNull(exp, "Expression for ?-Operator can't be null");
        }
    }

    record Plus(Expression exp) implements Operator {

        public Plus {
            Check.requireNotNull(exp, "Expression for +-Operator can't be null");
        }
    }

    record Group(Expression exp) implements Operator {

        public Group {
            Check.requireNotNull(exp, "Expression in Group can't be null");
        }
    }

    record QuestionMark(Expression exp) implements Operator {

        public QuestionMark {
            Check.requireNotNull(exp, "Expression for ?-Operator can't be null");
        }
    }

    record Not(Expression exp) implements Operator {

        public Not {
            Check.requireNotNull(exp, "Expression of !-Operator can't be null");
        }
    }
}
