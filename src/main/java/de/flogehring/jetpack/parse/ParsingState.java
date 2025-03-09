package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.MemoTable;
import lombok.Getter;

@Getter
public class ParsingState {

    private final MemoTable<MemoTableKey, ParsingStateLookup> lookup;

    private ParsingState() {
        lookup = MemoTable.of();
    }

    public static ParsingState of() {
        return new ParsingState();
    }
}
