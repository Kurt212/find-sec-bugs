package testcode;

import java.util.ArrayList;

public class OffByOne {
    void method0() {
        int arr[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; i <= arr.length; ++i) {
            int j = arr[i];
        }
    }
    void method2() {
        String s = "Hello World";
        for (int i = 0; i <= s.length(); ++i) {
            char c = s.charAt(i);
        }
    }
    void method3() {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(1);
        arr.add(2);
        arr.add(3);
        arr.add(4);
        arr.add(5);
        arr.add(6);
        for (int i = 0; i <= arr.size(); ++i) {
            int j = arr.get(i);
        }
    }
    void method4() {
        int arr[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; arr.length >= i; ++i) {
            int j = arr[i];
        }
    }
    void method5() {
        String s = "Hello World";
        for (int i = 0; s.length() >= i; ++i) {
            char c = s.charAt(i);
        }
    }
    void method6() {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(1);
        arr.add(2);
        arr.add(3);
        arr.add(4);
        arr.add(5);
        arr.add(6);
        for (int i = 0; arr.size() >= i; ++i) {
            Integer j = arr.get(i);
        }
    }
}
