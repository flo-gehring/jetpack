package de.friendlyhedgehog.jetpack.grammar;

import de.friendlyhedgehog.jetpack.util.Check;

public sealed interface Operator extends Expression {

    record Sequence(Expression first, Expression second) implements Operator {

        public Sequence {
            Check.requireNotNull("No Expression for Sequence can be null ", first, second);
        }
    }

    record OrderedChoice(Expression either, Expression or) implements Operator {

        public OrderedChoice {
            Check.requireNotNull("No Expression for Ordered Choice can be null", either, or);
        }
    }

    record Star(Expression exp) implements Operator {

        public Star {
            Check.requireNotNull("Expression for *-Operator can't be null", exp);
            Check.require(! (exp instanceof Star), "*-Operators can't be nested without quantifiable input e.g. no a**");
        }
    }

    record Optional(Expression exp) implements Operator {

        public Optional {
            Check.requireNotNull("Expression for ?-Operator can't be null", exp);
        }
    }

    record Plus(Expression exp) implements Operator {

        public Plus {
            Check.requireNotNull("Expression for +-Operator can't be null", exp);
        }
    }

    record Group(Expression exp) implements Operator {

        public Group {
            Check.requireNotNull("Expression in Group can't be null", exp);
        }
    }

    record Not(Expression exp) implements Operator {

        public Not {
            Check.requireNotNull("Expression of !-Operator can't be null", exp);
        }
    }

    record And(Expression exp) implements Operator {
        public And  {
            Check.requireNotNull("Expression of &-Operator can't be null");
        }
    }
}
