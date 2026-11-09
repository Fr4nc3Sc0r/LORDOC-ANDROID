package com.lordoc.data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Apre un file incluso nell'app a partire dal suo percorso relativo (es. "questions/index.txt").
 * Su Android è implementato con l'AssetManager; così la logica resta indipendente dalla piattaforma.
 */
public interface ResourceOpener {
    /** Restituisce il contenuto del file, oppure lancia IOException se non esiste. */
    InputStream open(String path) throws IOException;
}
