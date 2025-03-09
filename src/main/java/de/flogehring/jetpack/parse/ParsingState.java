package de.flogehring.jetpack.parse;

import lombok.Getter;

@Getter
public class ParsingState {

    private final MemoTable<ParsingStateLookup> lookup;

    private ParsingState() {
        lookup = MemoTable.of();
    }

    public static ParsingState of() {
        return new ParsingState();
    }
}
