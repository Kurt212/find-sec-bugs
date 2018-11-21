package com.h3xstream.findsecbugs;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/*

This detector spots 'off by one in for-loop' bug

currently it is working with

i <= array.length || array.length >= i
i <= string.length() || string.length() >= i
i <= ArrayList.size() || ArrayList.size() >= i

*/

public class OffByOneDetector extends OpcodeStackDetector {

    public OffByOneDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private BugReporter bugReporter;

    @Override
    public void visit(Code obj) {
        super.visit(obj);
    }

    private int pc_lastArrayLength = -1;
    private int pc_lastStringLength = -1;
    private int pc_lastArrayLostSize = -1;

    @Override
    public void sawOpcode(int seen) {

        if (seen == Const.ARRAYLENGTH) {
            pc_lastArrayLength = getPC();
        }

        if (seen == Const.INVOKEVIRTUAL && "java/lang/String".equals(getClassConstantOperand())
                && "length".equals(getNameConstantOperand())) {
            pc_lastStringLength = getPC();
        }

        if (seen == Const.INVOKEVIRTUAL && "java/util/ArrayList".equals(getClassConstantOperand())
                && "size".equals(getNameConstantOperand())) {
            pc_lastArrayLostSize = getPC();
        }

        if (stack.getStackDepth() >= 2
                && (seen == Const.IF_ICMPGT || seen == Const.IF_ICMPLT )) {
            // now we detected comparison
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);
            int r0 = item0.getRegisterNumber();
            int r1 = item1.getRegisterNumber();
            int rMin = Math.min(r0, r1);
            int rMax = Math.max(r0, r1);
            int jumpTarget = getBranchTarget(); // jump address if cmp is successful

            if (rMin == -1 && rMax > 0 && jumpTarget - 6 > getPC()) {
                int beforeTarget = getCodeByte(jumpTarget - 3); //
                int beforeGoto = getCodeByte(jumpTarget - 6);
                if (beforeTarget == Const.GOTO && beforeGoto == Const.IINC) {
                    // now we know we are in for loop

                    boolean implemented = false;

                    if (seen == Const.IF_ICMPGT) {
                        if (pc_lastArrayLength != -1 && pc_lastArrayLength == getPC() - 1) {
                            implemented = true;
                        }

                        if (pc_lastStringLength != -1 && pc_lastStringLength == getPC() - 3) {
                            implemented = true;
                        }

                        if (pc_lastArrayLostSize != -1 && pc_lastArrayLostSize == getPC() - 3) {
                            implemented = true;
                        }
                    } else {
                        if (pc_lastArrayLength != -1 && pc_lastArrayLength == getPC() - 2) {
                            implemented = true;
                        }

                        if (pc_lastStringLength != -1 && pc_lastStringLength == getPC() - 4) {
                            implemented = true;
                        }

                        if (pc_lastArrayLostSize != -1 && pc_lastArrayLostSize == getPC() - 4) {
                            implemented = true;
                        }
                    }

                    if (implemented) {
                        bugReporter.reportBug(new BugInstance(
                                this, "OFF_BY_ONE", NORMAL_PRIORITY
                        ).addClassAndMethod(this).addSourceLine(this));
                    }
                }
            }
        }
    }
}
