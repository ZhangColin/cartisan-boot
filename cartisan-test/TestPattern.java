import java.util.regex.Pattern;

public class TestPattern {
    public static void main(String[] args) {
        Pattern p = Pattern.compile(".*V\\d+.*");
        
        // Test cases
        String[] testCases = {
            "GoodExternalApiV1Controller",  // Should match
            "UserApiV2Controller",           // Should match
            "BadExternalApiController",      // Should NOT match
            "ApiV10",                        // Should match
            "V1Something",                   // Should match
            "NoVersion"                      // Should NOT match
        };
        
        for (String test : testCases) {
            System.out.println(test + " -> " + p.matcher(test).matches());
        }
    }
}
