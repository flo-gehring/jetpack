package de.friendlyhedgehog.jetpack.parse;

import de.friendlyhedgehog.jetpack.datatypes.MemoTable;
import lombok.Getter;

@Getter
class ParsingState {

    private final MemoTable<MemoTableKey, ParsingStateLookup> lookup;

    private ParsingState() {
        lookup = MemoTable.of();
    }

    public static ParsingState of() {
        return new ParsingState();
    }
}
