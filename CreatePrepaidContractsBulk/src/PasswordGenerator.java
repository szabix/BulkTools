import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!#&?";
    private ArrayList<String> charCategories;
    

    public PasswordGenerator() {
        charCategories = new ArrayList<String>(4);
        charCategories.add(LOWER);
        charCategories.add(UPPER);
        charCategories.add(DIGITS);
        charCategories.add(PUNCTUATION);
    }
    
    public String getPassword(int length) {
        // Variables.
        StringBuilder password = new StringBuilder(length);
        Random random = new Random(System.nanoTime());
    	
        for (int i = 0; i < length; i++) {
            String charCategory = charCategories.get(random.nextInt(charCategories.size()));
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }
    
    
}
