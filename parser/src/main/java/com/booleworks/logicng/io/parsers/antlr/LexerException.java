// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers.antlr;

/**
 * A lexer exception for the lexers.
 * @version 3.0.0
 * @since 1.0
 */
public final class LexerException extends RuntimeException {

    /**
     * Constructs a new lexer exception with a given message.
     * @param message the message
     */
    public LexerException(final String message) {
        super(message);
    }
}
