package com.h3xstream.findsecbugs;

import edu.umd.cs.findbugs.StatelessDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.generic.*;

import java.util.Arrays;

/*

This detector spots 'off by one in for-loop' bug

currently it is working with

i <= array.length || array.length >= i
i <= string.length() || string.length() >= i
i <= ArrayList.size() || ArrayList.size() >= i

*/

public class OffByOneDetector extends OpcodeStackDetector implements StatelessDetector {

    public OffByOneDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private BugReporter bugReporter;

    @Override
    public void visit(Code obj) {
        super.visit(obj);
    }

    private int findInstruction(int offset) {

        if (offset < 0) {
            return -1;
        }

        MethodGen g = getClassContext().getMethodGen(getMethod());
        InstructionList instList = g.getInstructionList();

        int[] allPositions = instList.getInstructionPositions();

        int idx = Arrays.binarySearch(allPositions, offset);
        if (idx < 0) {
            return -1;
        }
        return idx;
    }

    private Instruction getInstruction(int idx) {

        if (idx < 0) {
            return null;
        }

        MethodGen g = getClassContext().getMethodGen(getMethod());
        InstructionList instList = g.getInstructionList();

        return instList.getInstructions()[idx];
    }

    private Instruction getInstructionOffset(int offset) {
        int idx = findInstruction(offset);
        if (idx == -1) {
            return null;
        }

        return getInstruction(idx);
    }

    private enum bugType {
        EMPTY,
        ARRAY,
        STRING,
        ARRAYLIST
    };

    @Override
    public void sawOpcode(int seen) {
        ConstantPoolGen cpg = null;

        try {
            cpg =  new ConstantPoolGen(getMethod().getConstantPool());
        } catch (NullPointerException ex) {
            System.err.printf("Got NPE at [%s:%s]\n", getClassContext().getJavaClass().getClassName(), getMethod().getName());
            return;
        }


        if (stack.getStackDepth() >= 2 && (seen == Const.IF_ICMPGT || seen == Const.IF_ICMPLT )) {
            // now we detected comparison

            // make sure that one parameter is a variable
            // and another is not
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);

            int r0 = item0.getRegisterNumber();
            int r1 = item1.getRegisterNumber();

            int sizeRegister = Math.min(r0, r1);
            int VarRegister = Math.max(r0, r1);

            int jumpTarget = getBranchTarget(); // jump address if cmp is successful

            // if parameter is variable getRegisterNumber returns its register index
            // if is not - it returns -1
            if (sizeRegister == -1 && VarRegister > 0 && jumpTarget - 6 > getPC()) {
                // now we make sure that before jump target there is increment
                // and goto opcodes
                // this will let us know that we are in for loop
                int gotoOffset = getCodeByte(jumpTarget - 3);
                int incOffset = getCodeByte(jumpTarget - 6);

                if (gotoOffset == Const.GOTO && incOffset == Const.IINC) {
                    bugType type = bugType.EMPTY;

                    org.apache.bcel.generic.IINC incIns = (IINC) getInstructionOffset(jumpTarget - 6);

                    int increasedRegister = incIns.getIndex();

                    // check that variable used in comparison is used in increase
                    if (increasedRegister != VarRegister) {
                        return;
                    }

                    int containerRegister = -1;

                    // check if there was comparison with array.length, string.length() or ArrayList.size()
                    if (seen == Const.IF_ICMPGT) {
                        if (getCodeByte(getPC() - 1) == Const.ARRAYLENGTH) {
                            type = bugType.ARRAY;

                            Instruction ins = getInstruction(findInstruction(getPC() - 1) - 1);

                            if (ins instanceof ALOAD) {
                                ALOAD loadIns = (ALOAD) ins;
                                containerRegister = loadIns.getIndex();
                            }
                        } else if (getCodeByte(getPC() - 3) == Const.INVOKEVIRTUAL) {
                            int idx = findInstruction(getPC() - 3);
                            INVOKEVIRTUAL invokeIns = (INVOKEVIRTUAL) getInstruction(idx);

                            Instruction ins = getInstruction(idx - 1);

                            if (invokeIns.getClassName(cpg).equals("java.lang.String")
                                    && invokeIns.getMethodName(cpg).equals("length")) {
                                type = bugType.STRING;

                                if (ins instanceof ALOAD) {
                                    ALOAD loadIns = (ALOAD) ins;
                                    containerRegister = loadIns.getIndex();
                                }
                            }
                            if (invokeIns.getClassName(cpg).equals("java.util.ArrayList")
                                    && invokeIns.getMethodName(cpg).equals("size")) {
                                type = bugType.ARRAYLIST;

                                if (ins instanceof ALOAD) {
                                    ALOAD loadIns = (ALOAD) ins;
                                    containerRegister = loadIns.getIndex();
                                }
                            }
                        }
                    } else {
                        if (getCodeByte(getPC() - 2) == Const.ARRAYLENGTH) {
                            type = bugType.ARRAY;

                            Instruction ins = getInstruction(findInstruction(getPC() - 2) - 1);

                            if (ins instanceof ALOAD) {
                                ALOAD loadIns = (ALOAD) ins;
                                containerRegister = loadIns.getIndex();
                            }
                        } else if (getCodeByte(getPC() - 4) == Const.INVOKEVIRTUAL) {
                            int idx = findInstruction(getPC() - 4);
                            INVOKEVIRTUAL invokeIns = (INVOKEVIRTUAL) getInstruction(idx);

                            Instruction ins = getInstruction(idx - 1);

                            if (invokeIns.getClassName(cpg).equals("java.lang.String")
                                    && invokeIns.getMethodName(cpg).equals("length")) {
                                type = bugType.STRING;

                                if (ins instanceof ALOAD) {
                                    ALOAD loadIns = (ALOAD) ins;
                                    containerRegister = loadIns.getIndex();
                                }
                            }
                            if (invokeIns.getClassName(cpg).equals("java.util.ArrayList")
                                    && invokeIns.getMethodName(cpg).equals("size")) {
                                type = bugType.ARRAYLIST;

                                if (ins instanceof ALOAD) {
                                    ALOAD loadIns = (ALOAD) ins;
                                    containerRegister = loadIns.getIndex();
                                }
                            }
                        }
                    }

                    if (type == bugType.EMPTY) {
                        return;
                    }

                    // Detect that indexing is used in loop body

                    boolean isModified = false;
                    boolean isUsedForIndexing = false;

                    for (int i = getPC(); i < jumpTarget - 6; ++i) {

                        int code = getCodeByte(i);

                        boolean isIndexingIns = false;

                        int currentInsIdx = findInstruction(i);

                        Instruction thisInstruction = getInstruction(currentInsIdx);
                        if (thisInstruction == null) {
                            continue;
                        }

                        // detect indexing
                        if (type == bugType.ARRAY && (code == Const.AALOAD || code == Const.IALOAD
                                || code == Const.BALOAD || code == Const.CALOAD || code == Const.DALOAD
                                || code == Const.FALOAD || code == Const.LALOAD)) {

                            isIndexingIns = true;

                        } else if (type == bugType.STRING && code == Const.INVOKEVIRTUAL) {
                            INVOKEVIRTUAL ins = (INVOKEVIRTUAL) getInstructionOffset(i);
                            if (ins.getClassName(cpg).equals("java.lang.String")
                                    && ins.getMethodName(cpg).equals("charAt")) {
                                isIndexingIns = true;
                            }
                        } else if (type == bugType.ARRAYLIST && code == Const.INVOKEVIRTUAL) {
                            INVOKEVIRTUAL ins = (INVOKEVIRTUAL) getInstructionOffset(i);
                            if (ins.getClassName(cpg).equals("java.util.ArrayList")
                                    && ins.getMethodName(cpg).equals("get")) {
                                isIndexingIns = true;
                            }
                        }

                        if (isIndexingIns) {
                            Instruction prev1 = getInstruction(currentInsIdx - 1);
                            Instruction prev2 = getInstruction(currentInsIdx - 2);
                            if (prev1 instanceof ILOAD && prev2 instanceof ALOAD) {
                                ILOAD indexLoadIns = (ILOAD) prev1;
                                ALOAD containerLoadIns = (ALOAD) prev2;

                                if (indexLoadIns.getIndex() == VarRegister
                                        && containerLoadIns.getIndex() == containerRegister) {
                                    isUsedForIndexing = true;

                                }

                            }
                        }

                        //detect variable modification
                        if (code == Const.ISTORE || code == Const.ISTORE_0 || code == Const.ISTORE_1
                                || code == Const.ISTORE_2 || code == Const.ISTORE_3) {
                            ISTORE ins = (ISTORE) getInstructionOffset(i);
                            if (ins.getIndex() == VarRegister) {
                                isModified = true;
                            }
                        }

                    }

                    // make sure that variable is not modified in loop body
                    if (isModified) {
                        return;
                    }

                    if (isUsedForIndexing) {
                        bugReporter.reportBug(new BugInstance(
                                this, "OFF_BY_ONE", NORMAL_PRIORITY
                        ).addClassAndMethod(this).addSourceLine(this));
                    }
                }
            }
        }
    }
}
