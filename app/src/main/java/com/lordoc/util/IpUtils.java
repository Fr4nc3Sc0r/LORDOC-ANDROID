package com.lordoc.util;

/** Funzioni di utilità per indirizzi IPv4 (usate dal calcolatore e dal generatore di subnetting). */
public final class IpUtils {

    private IpUtils() { }

    /** Converte "a.b.c.d" in un long senza segno. */
    public static long toLong(String ip) {
        String[] p = ip.trim().split("\\.");
        if (p.length != 4) {
            throw new IllegalArgumentException("Indirizzo IPv4 non valido: " + ip);
        }
        long v = 0;
        for (String s : p) {
            int o;
            try {
                o = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ottetto non valido: " + s);
            }
            if (o < 0 || o > 255) {
                throw new IllegalArgumentException("Ottetto fuori range: " + s);
            }
            v = (v << 8) | o;
        }
        return v;
    }

    public static String toIp(long v) {
        return ((v >> 24) & 255) + "." + ((v >> 16) & 255) + "." + ((v >> 8) & 255) + "." + (v & 255);
    }

    public static long mask(int prefix) {
        checkPrefix(prefix);
        return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
    }

    public static String maskString(int prefix) { return toIp(mask(prefix)); }

    public static String wildcardString(int prefix) { return toIp(~mask(prefix) & 0xFFFFFFFFL); }

    public static long network(long ip, int prefix) { return ip & mask(prefix); }

    public static long broadcast(long ip, int prefix) { return network(ip, prefix) | (~mask(prefix) & 0xFFFFFFFFL); }

    /** Host utilizzabili: /31 = 2 (RFC 3021, link punto-punto), /32 = 1. */
    public static long usableHosts(int prefix) {
        checkPrefix(prefix);
        if (prefix == 32) {
            return 1;
        }
        if (prefix == 31) {
            return 2;
        }
        return (1L << (32 - prefix)) - 2;
    }

    /** Converte una maschera puntata (255.255.255.0) in prefisso, verificando che sia contigua. */
    public static int prefixFromMask(String mask) {
        long m = toLong(mask);
        int prefix = Long.bitCount(m);
        if (mask(prefix) != m) {
            throw new IllegalArgumentException("Maschera non contigua: " + mask);
        }
        return prefix;
    }

    /** Classe "classful" dell'indirizzo (A-E). */
    public static char ipClass(long ip) {
        int first = (int) ((ip >> 24) & 255);
        if (first < 128) {
            return 'A';
        }
        if (first < 192) {
            return 'B';
        }
        if (first < 224) {
            return 'C';
        }
        if (first < 240) {
            return 'D';
        }
        return 'E';
    }

    /** true se l'indirizzo appartiene agli spazi privati RFC 1918. */
    public static boolean isPrivate(long ip) {
        return network(ip, 8) == toLong("10.0.0.0")
            || network(ip, 12) == toLong("172.16.0.0")
            || network(ip, 16) == toLong("192.168.0.0");
    }

    public static String toBinary(long v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 31; i >= 0; i--) {
            sb.append((v >> i) & 1);
            if (i % 8 == 0 && i > 0) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    private static void checkPrefix(int prefix) {
        if (prefix < 0 || prefix > 32) {
            throw new IllegalArgumentException("Prefisso non valido: /" + prefix);
        }
    }
}
