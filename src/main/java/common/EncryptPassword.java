package common;

import org.jasypt.util.password.StrongPasswordEncryptor;

/**
 * Standalone utility to generate or verify Jasypt password hashes.
 *
 * Usage (from the project root after building):
 *   mvn dependency:copy-dependencies -DoutputDirectory=target/lib
 *   java -cp "target/lib/*:target/classes" common.EncryptPassword <plaintext>
 *   java -cp "target/lib/*:target/classes" common.EncryptPassword --check <plaintext> <hash>
 */
public class EncryptPassword {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("  Encrypt:  java common.EncryptPassword <plaintext>");
            System.err.println("  Verify:   java common.EncryptPassword --check <plaintext> <hash>");
            System.exit(1);
        }

        StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();

        if ("--check".equals(args[0])) {
            if (args.length < 3) {
                System.err.println("Usage: java common.EncryptPassword --check <plaintext> <hash>");
                System.exit(1);
            }
            boolean match = encryptor.checkPassword(args[1], args[2]);
            System.out.println(match ? "MATCH" : "NO MATCH");
            System.exit(match ? 0 : 1);
        } else {
            String hash = encryptor.encryptPassword(args[0]);
            System.out.println(hash);
        }
    }
}
