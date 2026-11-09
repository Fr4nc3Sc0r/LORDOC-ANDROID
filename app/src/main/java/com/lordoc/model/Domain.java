package com.lordoc.model;

/**
 * I sei domini (exam topics) del blueprint Cisco CCNA 200-301 v1.1
 * con il relativo peso percentuale all'esame.
 */
public enum Domain {
    NETWORK_FUNDAMENTALS("1.0", "Network Fundamentals", 20),
    NETWORK_ACCESS("2.0", "Network Access", 20),
    IP_CONNECTIVITY("3.0", "IP Connectivity", 25),
    IP_SERVICES("4.0", "IP Services", 10),
    SECURITY_FUNDAMENTALS("5.0", "Security Fundamentals", 15),
    AUTOMATION("6.0", "Automation and Programmability", 10);

    private final String code;
    private final String title;
    private final int weight;

    Domain(String code, String title, int weight) {
        this.code = code;
        this.title = title;
        this.weight = weight;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    /** Peso percentuale del dominio all'esame reale. */
    public int getWeight() { return weight; }

    public String getLabel() { return code + " " + title; }

    /** Accetta il nome dell'enum (NETWORK_ACCESS), il numero (2) o il codice (2.0). */
    public static Domain parse(String s) {
        String v = s.trim();
        for (Domain d : values()) {
            if (d.name().equalsIgnoreCase(v) || d.code.equals(v) || d.code.startsWith(v + ".")) {
                return d;
            }
        }
        throw new IllegalArgumentException("Dominio sconosciuto: " + s);
    }

    @Override
    public String toString() { return getLabel(); }
}
