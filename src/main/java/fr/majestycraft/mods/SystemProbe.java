package fr.majestycraft.mods;

import fr.trxyy.alternative.alternative_api.utils.OperatingSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Analyse de la machine pour le mode performance : processeur, mémoire, carte graphique
 * et système. Les commandes système (registre, lspci, sysctl...) sont bornées à quelques
 * secondes et n'ouvrent aucune fenêtre ; à appeler hors du thread JavaFX.
 */
public final class SystemProbe {

    public enum GpuKind { DEDICATED, INTEGRATED, SOFTWARE, UNKNOWN }

    public static final class Result {
        public int totalRamGb = 8;
        public double freeRamGb = -1;
        public int threads = Runtime.getRuntime().availableProcessors();
        public String cpu;
        public final List<String> gpus = new ArrayList<>();
        public String gpu;
        public GpuKind gpuKind = GpuKind.UNKNOWN;
        public String system;
        public boolean chromebook;
        public boolean arm;

        public String describeCpu() {
            String name = cpu != null ? cpu : "Processeur";
            return name + " · " + threads + " threads";
        }

        public String describeRam() {
            return totalRamGb + " Go" + (freeRamGb >= 0 ? " (" + formatGb(freeRamGb) + " Go libres)" : "");
        }

        public String describeGpu() {
            if (gpu == null) return "non détectée";
            switch (gpuKind) {
                case DEDICATED: return gpu + " · dédiée";
                case INTEGRATED: return gpu + " · intégrée";
                case SOFTWARE: return gpu + " · rendu logiciel";
                default: return gpu;
            }
        }

        /** Petite machine : peu de mémoire ou de cœurs, Chromebook, carte graphique intégrée ou logicielle. */
        public boolean isLowEnd() {
            return totalRamGb <= 8 || threads <= 4 || chromebook
                    || gpuKind == GpuKind.INTEGRATED || gpuKind == GpuKind.SOFTWARE;
        }

        /** Ancienne forme utilisée par le diagnostic de crash et le premier mode performance. */
        public PerformanceMode.Machine toMachine() {
            return new PerformanceMode.Machine(totalRamGb, threads, chromebook, arm);
        }
    }

    private SystemProbe() {
    }

    public static Result probe() {
        Result r = new Result();
        try {
            java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) bean;
                long total = os.getTotalPhysicalMemorySize();
                long free = os.getFreePhysicalMemorySize();
                if (total > 0) r.totalRamGb = (int) Math.max(1, Math.round(total / GB));
                if (free > 0) r.freeRamGb = free / GB;
            }
        } catch (Throwable ignored) {
        }
        r.chromebook = OperatingSystem.isChromeOS();
        r.arm = OperatingSystem.isHostArm64();
        r.system = describeSystem(r);
        r.cpu = detectCpu();
        r.gpus.addAll(detectGpus());
        pickGpu(r);
        return r;
    }

    private static final double GB = 1024.0 * 1024 * 1024;

    private static String describeSystem(Result r) {
        String name = r.chromebook ? "ChromeOS (Linux)" : System.getProperty("os.name", "?");
        // Java 8 annonce « Windows 10 » sous Windows 11 : le numéro de build tranche (22000 et plus)
        if (name.equals("Windows 10")) {
            for (String line : run(4, "reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "/v", "CurrentBuildNumber")) {
                int i = line.indexOf("REG_SZ");
                if (i < 0) continue;
                try {
                    if (Integer.parseInt(line.substring(i + 6).trim()) >= 22000) name = "Windows 11";
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String arch = System.getProperty("os.arch", "?").toLowerCase(Locale.ROOT);
        String bits = r.arm ? "ARM 64 bits" : arch.contains("64") ? "64 bits" : arch.equals("x86") ? "32 bits" : arch;
        return name + " · " + bits;
    }

    /* ─────────────────────────── Processeur ─────────────────────────── */

    private static String detectCpu() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                for (String line : run(4, "reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString")) {
                    int i = line.indexOf("REG_SZ");
                    if (i >= 0) return clean(line.substring(i + 6));
                }
            } else if (os.contains("mac")) {
                List<String> out = run(4, "sysctl", "-n", "machdep.cpu.brand_string");
                if (!out.isEmpty()) return clean(out.get(0));
            } else {
                File cpuinfo = new File("/proc/cpuinfo");
                if (cpuinfo.isFile()) {
                    for (String line : Files.readAllLines(cpuinfo.toPath(), StandardCharsets.UTF_8)) {
                        if (line.toLowerCase(Locale.ROOT).startsWith("model name")) return clean(line.substring(line.indexOf(':') + 1));
                    }
                }
                for (String line : run(4, "lscpu")) {
                    if (line.startsWith("Model name:")) return clean(line.substring(line.indexOf(':') + 1));
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /* ─────────────────────────── Carte graphique ─────────────────────────── */

    private static List<String> detectGpus() {
        List<String> gpus = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                for (String line : run(8, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        "Get-CimInstance Win32_VideoController | ForEach-Object { $_.Name }")) {
                    if (!line.trim().isEmpty()) gpus.add(clean(line));
                }
            } else if (os.contains("mac")) {
                for (String line : run(8, "system_profiler", "SPDisplaysDataType")) {
                    String t = line.trim();
                    if (t.startsWith("Chipset Model:")) gpus.add(clean(t.substring(t.indexOf(':') + 1)));
                }
            } else {
                for (String line : run(4, "lspci")) {
                    String l = line.toLowerCase(Locale.ROOT);
                    if (l.contains("vga compatible controller") || l.contains("3d controller") || l.contains("display controller")) {
                        String name = line.substring(line.indexOf(':', line.indexOf(' ')) + 1);
                        gpus.add(clean(name));
                    }
                }
                if (gpus.isEmpty()) {
                    for (String line : run(4, "glxinfo", "-B")) {
                        if (line.trim().startsWith("OpenGL renderer string:")) gpus.add(clean(line.substring(line.indexOf(':') + 1)));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return gpus;
    }

    /** Carte retenue : la dédiée si la machine en a une (portables à double carte). */
    private static void pickGpu(Result r) {
        for (GpuKind wanted : new GpuKind[]{GpuKind.DEDICATED, GpuKind.INTEGRATED, GpuKind.SOFTWARE, GpuKind.UNKNOWN}) {
            for (String g : r.gpus) {
                if (classify(g) == wanted) {
                    r.gpu = g;
                    r.gpuKind = wanted;
                    return;
                }
            }
        }
        if (r.chromebook) {
            r.gpu = "Carte graphique du Chromebook";
            r.gpuKind = GpuKind.INTEGRATED;
        }
    }

    static GpuKind classify(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("llvmpipe") || n.contains("softpipe") || n.contains("swrast") || n.contains("basic render")
                || n.contains("basic display")) {
            return GpuKind.SOFTWARE;
        }
        if (n.contains("geforce") || n.contains("rtx") || n.contains("gtx") || n.contains("quadro")
                || n.contains("radeon rx") || n.contains("radeon pro") || n.contains("radeon r9") || n.contains("radeon r7")
                || n.contains("intel arc") || n.matches(".*\\barc a\\d+.*") || n.contains("nvidia")) {
            return GpuKind.DEDICATED;
        }
        if (n.contains("intel") || n.contains("iris") || n.contains("uhd") || n.contains("radeon(tm) graphics")
                || n.contains("radeon graphics") || n.contains("vega") || n.contains("apple") || n.contains("adreno")
                || n.contains("mali") || n.contains("powervr") || n.contains("virgl") || n.contains("virtio")) {
            return GpuKind.INTEGRATED;
        }
        return GpuKind.UNKNOWN;
    }

    /* ─────────────────────────── Utilitaires ─────────────────────────── */

    /** Lance une commande (sans fenêtre) et renvoie ses lignes ; vide en cas d'échec ou de délai dépassé. */
    private static List<String> run(int timeoutSeconds, String... command) {
        List<String> lines = new ArrayList<>();
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            Thread reader = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    for (String l; (l = in.readLine()) != null; ) {
                        synchronized (lines) { lines.add(l); }
                    }
                } catch (Exception ignored) {
                }
            }, "MajestyLauncher-Probe");
            reader.setDaemon(true);
            reader.start();
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
            reader.join(500);
        } catch (Exception ignored) {
        }
        synchronized (lines) {
            return new ArrayList<>(lines);
        }
    }

    private static String clean(String s) {
        return s.replace("(R)", "").replace("(TM)", "").replace("(tm)", "").replace("CPU @", "@")
                .replaceAll("\\s+", " ").trim();
    }

    static String formatGb(double gb) {
        return gb >= 10 ? String.valueOf(Math.round(gb)) : String.format(Locale.FRANCE, "%.1f", gb);
    }
}
