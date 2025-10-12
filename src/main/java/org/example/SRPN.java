package org.example;

import java.io.IOException;
import java.util.*;

/**
 * Class for the SRPN calculator. Currently it outputs "0" for every "=" sign.
 * Use this class for your SRPN implementation.
 */

public class SRPN {

    private final LinkedList<String> userInput = new LinkedList<>(); // this acts more like a mini cache, to store values in and check previous values against to help guide the logic flow
    private final Stack<Integer> valueStack = new Stack<>();
    private final Stack<Integer> randomStack = new Stack<>(); // similar to userInput, acts as a cache in order to track the flow of the randomSequence numbers
    private final String[] operators = new String[]{"%", "-", "/", "+", "*", "^"};
    private final int[] randomSequence = {
            1804289383, 846930886, 1681692777, 1714636915, 1957747793,
            424238335, 719885386, 1649760492, 596516649, 1189641421,
            1025202362, 1350490027, 783368690, 1102520059, 2044897763,
            1967513926, 1365180540, 1540383426, 304089172, 1303455736,
            35005211, 521595368
    };
    private String operator;

    public void processCommand(String s) throws IOException {
        if (s.matches("^.{2,}$")) {  // matches more than 1 character
            String commentFreeString = s.replaceAll("#(.*?)#", "").replace("#","").trim();  // any characters caught between # will be removed, as well as the # signs, then remove all whitespaces
            if (!commentFreeString.isEmpty()){
                char[] chars = commentFreeString.toCharArray();
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

    private void processLine(char[] chars) throws IOException {
        List<String> word = new ArrayList<>();
        String character; // the full character
        for (int i = 0; i < chars.length; i++) {
            character = String.valueOf(chars[i]); // selects first char
            boolean isWhitespace = character.matches("\\s+");
            if (character.matches("\\d+")) {
                word.add(character);
                if (i == chars.length-1){
                    String number = stringBuilder(word);
                    userInput.add(number);
                    processStringItem(number);
                    word.clear();
                }
            } else if ((isOperator(character)) || isWhitespace) {
                if (i == 0){
                    word.add(character);
                } else {
                    if (!word.isEmpty()) {
                        String number = stringBuilder(word);
                        userInput.add(number);
                        processStringItem(number);
                        word.clear();
                    }
                    if (!isWhitespace){
                        userInput.add(character);
                        processStringItem(character);
                    }
                }
            } else {
                userInput.add(character);
                processStringItem(character);
            }
        }
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
                operator = s;
                handleOperation(operator); // performs calculation upon an operator being reached in the input chain
            } else if (s.matches("d")){
                valueStack.forEach(System.out::println); // handles an input of d which prints every value from the input thus far
            } else if (s.matches("r")){
                int index = randomStack.empty() ? 0 : randomStack.size(); // if the random stack is empty, index set to 0, otherwise it is equal to the size of the stack
                int value = (index < 22) ? randomSequence[index] : randomSequence[0]; // the value is determined by the index if the index val is under the overflow val, otherwise will return the first in the sequence
                // value pushed to both:
                randomStack.push(value); // 1: random stack - in order to track the order of numbers in parallel with user input
                valueStack.push(value); // 2: to the value stack - for calculation or output

            } else if (s.matches("=") && !hasReachedOverflow){
                boolean isPowerEqualsCombo = userInput.get(userInput.size()-2).equals("^");
                if (isPowerEqualsCombo) {
                    List<String> listOfInts = userInput.stream().filter(str -> str.matches("\\d+")).toList();
                    System.out.println(listOfInts.get(listOfInts.size()-1));
                } else {
                    System.out.println(valueStack.peek()); // if there is only 1 item left in the stack, this is the result
                }
            } else {
                System.out.println("Unrecognised operator or operand " + "\"" + s + "\"");
            }

        }
    }


    private void handleOperation(String operator) throws IOException {
        int numOfIntegers = valueStack.size(); // instantiate new local variable for readability rather than writing more verbose method call on valuestack field.
        int penultimate = userInput.size()-2; // int val of the penultimate position in the user input chain.
        boolean isValidOperation = (numOfIntegers >= 2 && !isOperator(userInput.get(penultimate))) ||
                (numOfIntegers >= 2 && isOperator(userInput.get(penultimate)) &&
                        userInput.get(penultimate - 1).matches("-?\\d+"));
        if (isValidOperation) { // at least 2 values must be present to perform calculation and the penultimate val must not be an operator
            int valB = valueStack.pop(); // returns the object at the top of the stack, which is the second number in the equation
            int valA = valueStack.pop(); // then returns value A, which is the first number in the equation
            if (isZero(valA, valB, operator)){ // handles situations where val a or b is 0 - reproduces srpn "Divide by 0." response
                System.out.println("Divide by 0.");
            } else if (operator.equals("^") && valB < 0){ // handles situations where value b is a negative integer - should reproduce srpn "Negative power." response
                System.out.println("Negative power.");
            } else {
                valueStack.push(performCalculation(valA, valB, operator)); // if neither of these negative cases are met, it will push the result performed by the calculation to the stack
            }
        } else {
            if (operator.equals("^") && numOfIntegers == 1) { // handles scenario where there is one integer in the stack and it is followed by a "^" operator
                if (valueStack.lastElement() > 0) { // ONLY performs n^2 if it is a number greater than 0
                    int val = valueStack.lastElement();
                    valueStack.push((val * val));
                }
            } else if (valueStack.lastElement() < 0) {
                System.out.println("Negative power."); // if it is less than 0, the output should be "Negative power"
            } else {
                System.out.println("Stack underflow."); // Any other scenario produces "Stack underflow."
            }
        }
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
                case "^" -> !exceedsMax ? (int) Math.pow(valueA, valueB) : Integer.MAX_VALUE;
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

    private boolean exceedsMinValue(int valA, int valB, String operator){
        return switch (operator) {
            case "*" -> (long) valA * (long) valB < Integer.MIN_VALUE;
            case "+" -> (long) valA + (long) valB < Integer.MIN_VALUE;
            case "-" -> (long) valA - (long) valB < Integer.MIN_VALUE;
            default -> false;
            // dividing two numbers can never reach below min val, neither can modulus
        };
    }

    private boolean isZero(int valA, int valB, String operator){
        return operator.equals("/") && valB == 0 || valA == 0;
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
