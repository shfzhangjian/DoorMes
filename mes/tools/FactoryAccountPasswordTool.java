import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Offline bootstrap helper: secrets arrive on stdin, never as command arguments. */
public class FactoryAccountPasswordTool {
    public static void main(String[] args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String password;
            while ((password = reader.readLine()) != null) {
                if (password.length() != 16) throw new IllegalArgumentException("Expected a generated 16-character password");
                System.out.println(BCrypt.hashpw(password, BCrypt.gensalt(10)));
            }
        }
    }
}
