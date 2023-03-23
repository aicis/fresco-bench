package dk.alexandra.benchmarking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Harness {
    private final Network network;
    private final int myId;

    private static final int ITERATIONS = 20;
    private static final int WARMUP = 5;

    /**
     * Launches the harness with the built-in program
     * @param args the ID for the given harness
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // TODO: add option for where to read 'parties.json' from
        Map<Integer, Party> parties = parsePartyList("parties.json");
        int myId = Integer.parseInt(args[0]);

        Harness harness = new Harness(myId, parties);
        List<Program> programs = new ArrayList<>();
        programs.add(new ExampleProgram());
        // TODO: add a way to add programs without modifying this class.

        List<String> results = new ArrayList<>();
        for (Program program : programs) {
            System.out.println("Running program: "+program.name());
            List<Long> times = harness.benchmark(program);
            System.out.println("Avg. time: "+Util.mean(times));
            System.out.println("std.: "+Util.std(times));
            results.add(Util.parseTimes(program.name(), times));
        }
        // TODO: Write results in a file (probably add a timestamp to the filename)
        // Util.writeResults("./", results);
        // TODO: Don't quit when done, but return to this state for awaiting further instructions
    }

    static private Map<Integer, Party> parsePartyList(String path) throws FileNotFoundException {
        Gson g = new Gson();
        Reader reader = new FileReader(path);
        Type clazz = TypeToken.getParameterized(List.class, Party.class).getType();
        List<Party> list = g.fromJson(reader, clazz);
        Map<Integer, Party> map = new HashMap<>(list.size());
        for (Party party : list) {
            map.put(party.getPartyId(), party);
        }
        return map;
    }

    public Harness(int myId, Map<Integer, Party> parties) {
        this.myId = myId;
        this.network = new SocketNetwork(new NetworkConfigurationImpl(myId, parties));
    }

    public List<Long> benchmark(Program program) {
        final byte[] READY = "ready".getBytes(StandardCharsets.UTF_8);
        // benchmark several sheets
        try {
            List<Long> times = new ArrayList<>(ITERATIONS);
            program.ready(network, myId);
            for (int i = 0; i < ITERATIONS + WARMUP; i++) {
                Thread.sleep(1);
                network.sendToAll(READY);
                network.receiveFromAll();
                long startTime = System.currentTimeMillis();
                program.run();
                long endTime = System.currentTimeMillis();
                if (i >= WARMUP) {
                    times.add(endTime - startTime);
                }
            }
            return times;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}