package org.example;

import java.io.IOException;
import java.util.*;

/**
 * Class for the SRPN calculator. Currently it outputs "0" for every "=" sign.
 * Use this class for your SRPN implementation.
 */

public class SRPN {

    /**
     * userInput a list used to store each user input. This is mostly used within the code like a cache:
        * Assists with scenarios like the ^= operation, where SRPN returns the last integer in the stack instead of doing a calculation
        * Keeps track of ALL recorded values
        * Instantiated as a final field because only operations will be performed on this ArrayList
            * It is important for the functionality of the field that it cannot be reassigned - otherwise the values could be replaced
     */
    private final LinkedList<String> userInput = new LinkedList<>();
    /**
     * valueStack holds the values in which calculations will be performed on, and/or outputted as the result
        * The Stack type is used as it allows utilisation of LIFO (last-in-first-out) logic
            * This allows for use of helpful FILO methods such as push(), pop(), peek(), which are used frequently in this program
            * Also means instead of having to find the last index as in ArrayLists (arr.size-1), you can simply use lastElement(), which enhances readability
     */
    private final Stack<Integer> valueStack = new Stack<>();
    private final Stack<Integer> randomStack = new Stack<>(); // similar to userInput, acts as a cache in order to track the flow of the randomSequence numbers

    /**
     * operators is a String array used for the sole purpose of validating the string input against these signs
        * Final as it should not be modified or reassigned.
     */
    private final String[] operators = new String[]{"%", "-", "/", "+", "*", "^"};

    /**
     * randomSequence is specifically designed to handle the 'r' case:
        * The set of integers displayed in the srpn program were the same each time and repeated in the same sequence
        * Therefore, it made sense to create an array:
            * Made it final as this is a fixed set of values that should not be changed, only accessed
            * int as they all sit within the integer range of -2^31 to 2^31-1.
            * Sorted so it can be iterated over
     */
    private final int[] randomSequence = {
            1804289383, 846930886, 1681692777, 1714636915, 1957747793,
            424238335, 719885386, 1649760492, 596516649, 1189641421,
            1025202362, 1350490027, 783368690, 1102520059, 2044897763,
            1967513926, 1365180540, 1540383426, 304089172, 1303455736,
            35005211, 521595368
    };

    private final SRPNUtil srpnUtility = new SRPNUtil();

    public void processCommand(String s) throws IOException {
        if (s.matches("^.{2,}$")) {  // matches more than 1 character

            // need to process 2 scenarios here regarding whitespaces:
            // negative numbers - spaces between '-' operator and digit are crucial
            // same for positive - +4 will actually add them to the previous number, so needs to be placed AFTER the number in processing
            // for ^=, the last digit will be printed but not added, whilst the rest of the equation will execute as normal
            char[] chars = s.toCharArray();

            for (int i = chars.length-1; i > 0; i--) {
                if (chars[i] == '=' && chars[i-1] == '^') {
                    char precedingChar = chars[i-2];
                    if (Character.isDigit(precedingChar)){
                        s = s.replace("=", "");
                        System.out.println(chars[precedingChar]);
                    } else if (Character.isWhitespace(precedingChar) && Character.isDigit(chars[i-3])){
                        s = s.replace("=", "");
                        System.out.println(chars[i-3]);
                    }
                }
            }


            String commentFreeString = s.replaceAll("#(.*?)#", "").replace("#","").trim();  // any characters caught between # will be removed, as well as the # signs, then trim
            if (!commentFreeString.isEmpty()){
                chars = commentFreeString.toCharArray();
                if (chars.length == 1){ //checks if it is a single digit or single character
                    userInput.add(s); // add it to the "cached" history of inputs
                    processStringItem(s); // if it is, it can process the char / digit
                }
                processLine(chars);
            }
        } else {
            userInput.add(s);
            processStringItem(s);
        }
    }

    // two things to add:
    //1: scenario with ^= - processes at the LINE level
//    boolean isPowerEqualsCombo = userInput.get(userInput.size()-2).equals("^"); // in scenarios in which ^= are recorded, the calculator will print the last recorded value
//                    if (isPowerEqualsCombo) {
//                        System.out.println(valueStack.peek()); // prints the last value in the stack
//
//

    private void processLine(char[] chars) throws IOException {
        List<String> word = new ArrayList<>();

        for (int i = 0; i < chars.length; i++) {
            char character = chars[i]; // selects first char
            boolean isWhitespace = Character.isWhitespace(character);
            boolean isDigit = Character.isDigit(character);
            boolean nextIsDigit = i + 1 < chars.length && Character.isDigit(chars[i + 1]); // makes sure next char is digit and has not reached end of array


            if (isDigit) {
                word.add(String.valueOf(character)); // will add digit to a word "list" until either an operator or whitespace is reached
                if (i == chars.length-1) { // if i reaches the end of the chain, will save and process the final char
                    buildAndFlushWord(word);
                }
            } else if (isOperator(String.valueOf(character)) || isWhitespace) {
                if (i != 0 && !word.isEmpty()) { // if its not the first char and the word is not empty, it will save and process the char, then move on
                    buildAndFlushWord(word);
                }
                if (!isWhitespace) { // if it is not a whitespace, it must be an operator (by default), so it saves and processes the char

                    if (character == '-' && nextIsDigit){
                        word.add(String.valueOf(character)); // for negative values, will not process '-' character as a subtraction
                    } else {

                        if (character == '+' && nextIsDigit){ // if there is a + sign prior to digit with no whitespace in between, treat as addition functionality
                            String digit = String.valueOf(chars[i+1]); // processes the digit first
                            userInput.add(digit);
                            processStringItem(digit);
                            i++; // now that digit has been processed, needs to be skipped in the loop
                        }
                        // THEN processes operator
                        String operator = String.valueOf(character);
                        userInput.add(operator);
                        processStringItem(operator);
                    }
                }
            } else {
                String otherChar = String.valueOf(character); // any other char will be processed at the next stage
                userInput.add(otherChar);
                processStringItem(otherChar);
            }
        }
    }

    private void buildAndFlushWord(List<String> word) throws IOException {
        String number = stringBuilder(word);
        userInput.add(number);
        processStringItem(number);
        word.clear();
    }

    private String stringBuilder(List<String> word) throws IOException {
        StringBuilder wordBuilder = new StringBuilder();
        for (String s : word) {
            wordBuilder.append(s);
        }
        return wordBuilder.toString();
    }

    private void processStringItem(String s) throws IOException {
        boolean hasReachedOverflow = valueStack.size() == 23; // maximum stack size is 23, so anything lower than this can be pushed
        boolean isNumber = s.matches("-?\\d+") || s.matches("r");
        if (hasReachedOverflow && isNumber){
            System.out.println("Stack overflow.");
        } else {
            if (s.matches("-?\\d+")){
                valueStack.push(Integer.parseInt(s));
            } else if (isOperator(s) && !hasReachedOverflow){ // once an operator is reached, it will perform a calculation
                handleOperation(s); // performs calculation upon an operator being reached in the input chain
            } else if (s.matches("d")){
                if (valueStack.empty()){
                    System.out.println(Integer.MIN_VALUE); // Scenario where there are no values present, inputting d will print min value
                }
                valueStack.forEach(System.out::println); // handles an input of d which prints every value from the input thus far
            } else if (s.matches("r")){
                int index = randomStack.empty() ? 0 : randomStack.size(); // if the random stack is empty, index set to 0, otherwise it is equal to the size of the stack
                int value = (index < 22) ? randomSequence[index] : randomSequence[0]; // the value is determined by the index if the index val is under the overflow val, otherwise will return the first in the sequence
                // value pushed to both:
                randomStack.push(value); // 1: random stack - in order to track the order of numbers in parallel with user input
                valueStack.push(value); // 2: to the value stack - for calculation or output

            } else if (s.matches("=") && !hasReachedOverflow){
                if (valueStack.empty()){
                    System.out.println("Stack empty."); // if = sign and no values in the stack, prints "Stack empty."
                } else {
                    System.out.println(valueStack.peek()); // if there is only 1 item left in the stack, this is the result
                }
            } else {
                System.out.println("Unrecognised operator or operand " + "\"" + s + "\"");
            }

        }
    }

    /**
     * This method
     * This method will only "pop" the values from the stack IF a valid calculation can be performed, i.e. if it's not a negative power, or dividing by zero.
     * @param operator
     */

    private void handleOperation(String operator) throws IOException {
        if (isValidOperation()) { // at least 2 values must be present to perform calculation and the penultimate val must not be an operator
            if (isResultZero(valueStack.get(valueStack.size()-2), valueStack.peek(), operator)){
                System.out.println("Divide by 0.");
            } else if (operator.equals("^") && valueStack.peek() < 0){ // handles situations where value b is a negative integer - should reproduce srpn "Negative power." response
                System.out.println("Negative power.");
            } else {
                int valB = valueStack.pop(); // returns the object at the top of the stack, which is the second number in the equation
                int valA = valueStack.pop(); // then returns value A, which is the first number in the equation
                valueStack.push(performCalculation(valA, valB, operator)); // if neither of these negative cases are met, it will push the result performed by the calculation to the stack
            }
        } else {
            System.out.println("Stack underflow."); // Any other scenario produces "Stack underflow."
        }
    }

    private boolean isResultZero(int valueA, int valueB, String operator){
        return (valueA == 0 || valueB == 0) && operator.equals("/");
    }

    /**
     * isValidOperation returns a boolean value based on the current state of the userInput and valueStack collections
        * Encapsulating the logic in a separate method: allows the details of the validation to be separated from its caller method, also resulting in better readability
     * The method has two key components to its validation:
        * 1: if the penultimate value from userInput is NOT an operator and the number of values in the stack are equal to or greater than 2, its a valid operation
            * Any less than 2 values
        * 2: if the penultimate value from userInput IS an operator AND the value preceding that matches the regex pattern of a digit (negative or positive), it is also valid
        * 3: a minimum stack size value of 2 must be present for the operation to be valid in both cases
     */
    private boolean isValidOperation() {
        int numOfIntegers = valueStack.size(); // instantiate new local variable for readability rather than writing more verbose method call on valueStack field.
        int penultimate = userInput.size()-2; // int val of the penultimate position in the user input chain.
        return numOfIntegers >= 2;
    }

    private int performCalculation(int valueA, int valueB, String operator) throws IOException {
        boolean isMax = Integer.MAX_VALUE == valueA || Integer.MAX_VALUE == valueB;
        boolean isMin = Integer.MIN_VALUE == valueA || Integer.MIN_VALUE == valueB;
        boolean exceedsMax = exceedsMaxValue(valueA, valueB, operator);
        boolean exceedsMin = exceedsMinValue(valueA, valueB, operator);
            return switch (operator) {
                case "%" -> !isMax && !isMin ? valueA % valueB  // if not max or min values, return the remainder of the operation
                                : isMax ? Integer.MAX_VALUE : Integer.MIN_VALUE; // if not, return max value if one val = max val, or return min value otherwise
                case "*" ->  !exceedsMax && !exceedsMin //
                                ? valueA * valueB : (isMax || exceedsMax) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                case "/" -> !exceedsMax ? valueA / valueB : isMax ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                case "-" -> !exceedsMax && !exceedsMin
                                ? valueA - valueB : (isMax || exceedsMax) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                case "^" -> !exceedsMax && !exceedsMin
                                ? (int) Math.pow(valueA, valueB) : (isMax || exceedsMax) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                default -> !exceedsMax && !exceedsMin
                                ? valueA + valueB : (isMax || exceedsMax) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            };
    }

    private boolean isOperator(String input){
        boolean isOperator = false;
        for (String string : operators) {
            if (string.equals(input)){
                isOperator = true;
                break;
            }
        }
        return isOperator;
    }

    /**
     * Function designed to take two integers and an operator and return true or false based on whether the result of the calculation exceeds the minimum integer value
        * Only takes into account subtraction, addition and multiplication
        * Values must be cast to long as the result must be compared to the min value of an integer (-2^31), so must be able to pass this threshold to validate the boolean condition
        * Dividing two numbers can never reach below min val, neither can modulus, so these are excluded
     */
    private boolean exceedsMinValue(int valA, int valB, String operator){
        return switch (operator) {
            case "*" -> (long) valA * (long) valB < Integer.MIN_VALUE;
            case "+" -> (long) valA + (long) valB < Integer.MIN_VALUE;
            case "-" -> (long) valA - (long) valB < Integer.MIN_VALUE;
            case "^" -> (long) Math.pow(valA, valB) < Integer.MIN_VALUE; // Utilise the pow() function to process the result
            default -> false;
        };
    }

    private boolean exceedsMaxValue(int valA, int valB, String operator) {
        // casting each value to long so that the calculation can occur
        return switch (operator) {
            case "*" -> (long) valA * (long) valB > Integer.MAX_VALUE;
            case "/" -> (long) valA / (long) valB > Integer.MAX_VALUE; // dividing two numbers can reach above max val in one case - Min val / -1
            case "+" -> (long) valA + (long) valB > Integer.MAX_VALUE;
            case "-" -> (long) valA - (long) valB > Integer.MAX_VALUE;
            case "^" -> (long) Math.pow(valA, valB) > Integer.MAX_VALUE;
            default -> false;
            // two numbers applied against the modulus operator can never reach max val
        };
    }
}
