package com.lordoc.data;

import com.lordoc.model.Domain;
import com.lordoc.model.Question;
import com.lordoc.model.QuestionType;
import com.lordoc.util.IpUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Genera domande di subnetting IPv4 sempre nuove, con la spiegazione passo-passo
 * del metodo del "magic number" (block size).
 */
public final class SubnetQuestionGenerator {

    private static final String REF = "CCNA 200-301 exam topic 1.6 – Configure and verify IPv4 addressing and subnetting";

    private final Random random;
    private int counter;

    public SubnetQuestionGenerator(Random random) {
        this.random = random;
    }

    public List<Question> generate(int count) {
        List<Question> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateOne());
        }
        return list;
    }

    public Question generateOne() {
        counter++;
        int variant = random.nextInt(6);
        switch (variant) {
            case 0: return networkAddress();
            case 1: return broadcastAddress();
            case 2: return lastUsable();
            case 3: return hostsPerSubnet();
            case 4: return maskForHosts();
            default: return validHost();
        }
    }

    // ------------------------------------------------------------------ helpers

    private int randomPrefix(int min, int max) { return min + random.nextInt(max - min + 1); }

    private long randomIp(int prefix) {
        long base;
        if (prefix >= 24) {
            switch (random.nextInt(3)) {
                case 0: base = IpUtils.toLong("10.0.0.0") | ((long) random.nextInt(1 << 24)); break;
                case 1: base = IpUtils.toLong("172.16.0.0") | ((long) random.nextInt(1 << 20)); break;
                default: base = IpUtils.toLong("192.168.0.0") | ((long) random.nextInt(1 << 16)); break;
            }
        } else if (random.nextBoolean()) {
            base = IpUtils.toLong("10.0.0.0") | ((long) random.nextInt(1 << 24));
        } else {
            base = IpUtils.toLong("172.16.0.0") | ((long) random.nextInt(1 << 20));
        }
        long net = IpUtils.network(base, prefix);
        long bc = IpUtils.broadcast(base, prefix);
        // un host valido (né rete né broadcast)
        long span = bc - net - 1;
        return net + 1 + (long) (random.nextDouble() * span);
    }

    private Question.Builder base(QuestionType type) {
        return new Question.Builder()
            .id(String.format("GEN-SUB-%04d", counter))
            .domain(Domain.NETWORK_FUNDAMENTALS)
            .type(type)
            .generated(true)
            .source("generatore")
            .reference(REF);
    }

    /** Spiegazione col metodo del magic number. */
    private static String method(long ip, int prefix) {
        int octetIndex = (prefix - 1) / 8;           // 0..3: ottetto "interessante"
        long mask = IpUtils.mask(prefix);
        int maskOctet = (int) ((mask >> (24 - 8 * octetIndex)) & 255);
        int block = 256 - maskOctet;
        int ipOctet = (int) ((ip >> (24 - 8 * octetIndex)) & 255);
        int netOctet = (ipOctet / block) * block;
        long net = IpUtils.network(ip, prefix);
        long bc = IpUtils.broadcast(ip, prefix);
        StringBuilder sb = new StringBuilder();
        sb.append("Metodo del \"magic number\":\n");
        sb.append("1) /").append(prefix).append(" = maschera ").append(IpUtils.maskString(prefix)).append(".\n");
        sb.append("2) L'ottetto interessante è il ").append(octetIndex + 1)
          .append("° (valore di maschera ").append(maskOctet).append(").\n");
        sb.append("3) Block size = 256 - ").append(maskOctet).append(" = ").append(block)
          .append(": le subnet partono da multipli di ").append(block).append(".\n");
        sb.append("4) Nel ").append(octetIndex + 1).append("° ottetto l'host vale ").append(ipOctet)
          .append(", che cade nel blocco ").append(netOctet).append("–").append(netOctet + block - 1).append(".\n");
        sb.append("5) Rete: `").append(IpUtils.toIp(net)).append("`  Broadcast: `").append(IpUtils.toIp(bc))
          .append("`\n   Host utilizzabili: `").append(IpUtils.toIp(net + 1)).append("` – `")
          .append(IpUtils.toIp(bc - 1)).append("` (").append(IpUtils.usableHosts(prefix)).append(" host).");
        return sb.toString();
    }

    // ------------------------------------------------------------------ varianti

    private Question networkAddress() {
        int p = randomPrefix(17, 30);
        long ip = randomIp(p);
        String net = IpUtils.toIp(IpUtils.network(ip, p));
        return base(QuestionType.FILL)
            .text("What is the subnet (network) address of host " + IpUtils.toIp(ip) + "/" + p + "?\n"
                + "Enter the address in dotted-decimal format.")
            .accept(net).accept(net + "/" + p)
            .explanation(method(ip, p))
            .build();
    }

    private Question broadcastAddress() {
        int p = randomPrefix(17, 30);
        long ip = randomIp(p);
        String bc = IpUtils.toIp(IpUtils.broadcast(ip, p));
        return base(QuestionType.FILL)
            .text("A host is configured with IP address " + IpUtils.toIp(ip) + " and subnet mask "
                + IpUtils.maskString(p) + ". What is the broadcast address of its subnet?")
            .accept(bc)
            .explanation(method(ip, p) + "\n\nTrucco: il broadcast è sempre (indirizzo della subnet successiva) - 1.")
            .build();
    }

    private Question lastUsable() {
        int p = randomPrefix(20, 30);
        long ip = randomIp(p);
        String last = IpUtils.toIp(IpUtils.broadcast(ip, p) - 1);
        return base(QuestionType.FILL)
            .text("What is the last usable host address in the subnet that contains " + IpUtils.toIp(ip)
                + "/" + p + "?")
            .accept(last)
            .explanation(method(ip, p) + "\n\nL'ultimo host utilizzabile è il broadcast meno 1.")
            .build();
    }

    private Question hostsPerSubnet() {
        int p = randomPrefix(18, 30);
        long hosts = IpUtils.usableHosts(p);
        int hostBits = 32 - p;
        boolean useMask = random.nextBoolean();
        String spec = useMask ? "subnet mask " + IpUtils.maskString(p) : "a /" + p + " prefix";
        return base(QuestionType.FILL)
            .text("How many usable host addresses are available in a subnet with " + spec + "?")
            .accept(String.valueOf(hosts))
            .explanation("Bit di host = 32 - " + p + " = " + hostBits + ".\n"
                + "Host utilizzabili = 2^" + hostBits + " - 2 = " + (1L << hostBits) + " - 2 = " + hosts + ".\n\n"
                + "Si sottraggono 2 indirizzi: quello di rete (tutti i bit di host a 0) e il broadcast "
                + "(tutti i bit di host a 1). Eccezione: /31 sui link punto-punto (RFC 3021) usa entrambi gli indirizzi.")
            .build();
    }

    private Question maskForHosts() {
        int targetPrefix = randomPrefix(20, 29);
        long max = IpUtils.usableHosts(targetPrefix);
        long min = IpUtils.usableHosts(targetPrefix + 1) + 1;
        long needed = min + (long) (random.nextDouble() * (max - min + 1));
        Set<Integer> prefixes = new LinkedHashSet<>();
        prefixes.add(targetPrefix);
        prefixes.add(targetPrefix + 1);
        prefixes.add(targetPrefix - 1);
        prefixes.add(targetPrefix + 2 <= 30 ? targetPrefix + 2 : targetPrefix - 2);
        List<Integer> opts = new ArrayList<>(prefixes);
        Collections.shuffle(opts, random);
        Question.Builder b = base(QuestionType.SINGLE)
            .text("A network engineer must create a subnet that supports " + needed + " hosts and wastes "
                + "the fewest addresses. Which subnet mask should be used?");
        for (int i = 0; i < opts.size(); i++) {
            int op = opts.get(i);
            b.option(IpUtils.maskString(op) + " (/" + op + ")");
            if (op == targetPrefix) {
                b.correct(i);
            }
        }
        int hb = 32 - targetPrefix;
        return b.explanation("Serve il più piccolo n tale che 2^n - 2 ≥ " + needed + ".\n"
                + "2^" + (hb - 1) + " - 2 = " + IpUtils.usableHosts(targetPrefix + 1) + " (insufficiente)\n"
                + "2^" + hb + " - 2 = " + max + " (sufficiente)\n"
                + "Quindi " + hb + " bit di host → /" + targetPrefix + " = " + IpUtils.maskString(targetPrefix) + ".")
            .build();
    }

    private Question validHost() {
        int p = randomPrefix(24, 29);
        long ip = randomIp(p);
        long net = IpUtils.network(ip, p);
        long bc = IpUtils.broadcast(ip, p);
        long size = bc - net + 1;
        List<long[]> candidates = new ArrayList<>(); // {address, isValid}
        candidates.add(new long[] {ip, 1});
        candidates.add(new long[] {net, 0});
        candidates.add(new long[] {bc, 0});
        candidates.add(new long[] {bc + 1 + random.nextInt((int) Math.max(1, size - 2)), 0}); // subnet successiva
        Collections.shuffle(candidates, random);
        Question.Builder b = base(QuestionType.SINGLE)
            .text("Which address is a valid host address in subnet " + IpUtils.toIp(net) + "/" + p + "?");
        for (int i = 0; i < candidates.size(); i++) {
            b.option(IpUtils.toIp(candidates.get(i)[0]));
            if (candidates.get(i)[1] == 1) {
                b.correct(i);
            }
        }
        return b.explanation(method(ip, p)
                + "\n\nLe opzioni errate sono l'indirizzo di rete, il broadcast oppure un host di un'altra subnet.")
            .build();
    }
}
