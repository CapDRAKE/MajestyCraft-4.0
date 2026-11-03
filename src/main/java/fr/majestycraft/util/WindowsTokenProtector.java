package fr.majestycraft.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import fr.trxyy.alternative.alternative_auth.base.AuthConfig;

/**
 * Chiffre le fichier des jetons Microsoft avec DPAPI, comme le launcher officiel : seule la
 * session Windows qui l'a écrit peut le relire. Copié sur un autre PC (ou envoyé par un
 * logiciel espion qui ne fait que ramasser des fichiers), il est inutilisable.
 */
public final class WindowsTokenProtector implements AuthConfig.TokenProtector {

    /** À installer au démarrage, avant toute lecture des jetons. Sans effet hors Windows. */
    public static void install() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            AuthConfig.protector = new WindowsTokenProtector();
        }
    }

    @Override
    public String protect(String json) throws IOException {
        return Base64.getEncoder().encodeToString(Dpapi.run(true, json.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String unprotect(String payload) throws IOException {
        return new String(Dpapi.run(false, Base64.getDecoder().decode(payload.trim())), StandardCharsets.UTF_8);
    }

    /** Chargé au premier usage seulement : rien n'est chargé pour un compte hors ligne. */
    private static final class Dpapi {

        private static final int CRYPTPROTECT_UI_FORBIDDEN = 0x1;
        private static final byte[] ENTROPY = "MajestyCraft-auth".getBytes(StandardCharsets.UTF_8);

        interface Crypt32 extends StdCallLibrary {
            Crypt32 INSTANCE = (Crypt32) Native.loadLibrary("Crypt32", Crypt32.class);

            boolean CryptProtectData(Blob in, WString description, Blob entropy, Pointer reserved,
                                     Pointer prompt, int flags, Blob out);

            boolean CryptUnprotectData(Blob in, PointerByReference description, Blob entropy, Pointer reserved,
                                       Pointer prompt, int flags, Blob out);
        }

        interface Kernel32 extends StdCallLibrary {
            Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);

            Pointer LocalFree(Pointer memory);
        }

        /** DATA_BLOB */
        public static class Blob extends Structure {
            public int cbData;
            public Pointer pbData;

            public Blob() {
            }

            Blob(byte[] data) {
                pbData = new Memory(Math.max(1, data.length));
                pbData.write(0, data, 0, data.length);
                cbData = data.length;
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("cbData", "pbData");
            }
        }

        static byte[] run(boolean protect, byte[] data) throws IOException {
            Blob in = new Blob(data), entropy = new Blob(ENTROPY), out = new Blob();
            boolean ok = protect
                    ? Crypt32.INSTANCE.CryptProtectData(in, new WString("MajestyCraft"), entropy, null, null, CRYPTPROTECT_UI_FORBIDDEN, out)
                    : Crypt32.INSTANCE.CryptUnprotectData(in, null, entropy, null, null, CRYPTPROTECT_UI_FORBIDDEN, out);
            if (!ok) {
                throw new IOException("DPAPI : erreur Windows " + Native.getLastError());
            }
            try {
                return out.pbData.getByteArray(0, out.cbData);
            } finally {
                Kernel32.INSTANCE.LocalFree(out.pbData);
            }
        }
    }
}
