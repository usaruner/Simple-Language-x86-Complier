package crux.backend;

import crux.frontend.types.VoidType;
import crux.midend.ir.core.*;
import crux.midend.ir.core.insts.*;
import crux.printing.IRValueFormatter;

import javax.sound.midi.SysexMessage;
import java.util.*;

public final class CodeGen extends InstVisitor {
    private final IRValueFormatter irFormat = new IRValueFormatter();

    private final Program p;
    private final CodePrinter out;
    private final String codeSize = "8";
    private final String[] InitArgs = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};
    public CodeGen(Program p) {
        this.p = p;
        // Do not change the file name that is outputted or it will
        // break the grader!
        
        out = new CodePrinter("a.s");
    }

    public void genCode() {
        //This function should generate code for the entire program.
        Iterator globV = p.getGlobals();
        Iterator globF = p.getFunctions();
        GlobalDecl temp;
        String name;
        Value size;
        Function func;
        while(globV.hasNext()){
            temp = (GlobalDecl)globV.next();
            name = temp.getAllocatedAddress().getName().replace("%","");
            size = temp.getNumElement();
            out.bufferCode(".comm " + name + ", " + irFormat.apply(size) + ", " + codeSize);
            out.printCode(".comm " + name + ", " + irFormat.apply(size) + ", " + codeSize);
            System.out.println("SIZE: " + ".comm " + name + ", " + irFormat.apply(size) + ", " + codeSize);
        }
        while(globF.hasNext()){
            func = (Function)globF.next();
            genCode(func);
        }
        out.close();
    }

    private int labelcount = 1;

    private String getNewLabel() {
        return "L" + (labelcount++);
    }
    private String convLocAddr(LocalVar t){
        int num = (8*(1+ Integer.parseInt(t.toString().replace("$t",""))));
        return ("-" + num);
    }
    private String convAddr(AddressVar t) {
        int num = (8*(1+ Integer.parseInt(t.toString().replace("%t",""))));
        return ("-" + num);
    }
    private void visit(Instruction inst){
        if(inst.getClass() == AddressAt.class) {
            visit((AddressAt)inst);
        }else if(inst.getClass() == BinaryOperator.class) {
            visit((BinaryOperator) inst);
        }else if(inst.getClass() == CallInst.class) {
            visit((CallInst) inst);
        }else if(inst.getClass() == CompareInst.class) {
            visit((CompareInst) inst);
        }else if(inst.getClass() == CopyInst.class) {
            visit((CopyInst) inst);
        }else if(inst.getClass() == JumpInst.class) {
            visit((JumpInst) inst);
        }else if(inst.getClass() == LoadInst.class) {
            visit((LoadInst) inst);
        }else if(inst.getClass() == NopInst.class) {
            visit((NopInst) inst);
        }else if(inst.getClass() == ReturnInst.class) {
            visit((ReturnInst) inst);
        }else if(inst.getClass() == StoreInst.class) {
            visit((StoreInst) inst);
        }else if(inst.getClass() == UnaryNotInst.class) {
            visit((UnaryNotInst) inst);
        }
    }
    private void genCode(Function f) {
        //Assign labels for jump targets
        HashMap<Instruction, String> temp = assignLabels(f);
        //Declare functions and print label    “.globl main”      “main:”
        System.out.println("Name "+ f.getName() + "t/f" + f.getName() == "main");
        if(f.getName().equals("main")){
            out.bufferCode(".globl main");
            out.printCode(".globl main");
        }
        //print label
        out.bufferCode(f.getName() + ":");
        out.printCode(f.getName() + ":");
        //Emit functions prologue
        out.bufferCode("\tenter $(" + 16+ "+"+Integer.parseInt(codeSize) + "*" + (f.getArguments().size()+1) + ")" + ", $0");
        out.printCode("\tenter $(" + 16+ "+" +Integer.parseInt(codeSize) + "*" + (f.getArguments().size()+1) + ")" + ", $0");
        //For functions Arguments
        if(f.getArguments().size() > 6) {
            for (int i = 6; i < f.getArguments().size(); i++) {
                out.bufferCode("\tmovq " + (-8*(i+1)) + "(%rbp)" + ", " + (-8*(i+1)) + "(%rbp)");
                out.printCode("\tmovq " + (-8*(i+1)) + "(%rbp)" + ", " + (-8*(i+1)) + "(%rbp)");
            }
        }
        for(int i = 0; i < f.getArguments().size(); i++){
            if(f.getArguments().size() <= 5) {
                out.bufferCode("\tmovq " + InitArgs[i] + ", " + (-8*(i+1)) + "(%rbp)");
                out.printCode("\tmovq " + InitArgs[i] + ", " + (-8*(i+1)) + "(%rbp)");
            }
        }
        Instruction inst = f.getStart();
        while(inst.numNext() != 0){
            visit(inst);
            inst = inst.getNext(0);
        }
        Instruction key;
        String lab;
        for (Map.Entry< Instruction, String> entry : temp.entrySet()) {
            key = entry.getKey();
            lab = entry.getValue();
            out.bufferCode(lab + ":");
            out.printCode(lab + ":");
            while(key.numNext() != 0){
                visit(key);
                key = key.getNext(0);
            }
        }
        out.bufferCode("\tleave");
        out.printCode("\tleave");
        out.bufferCode("\tret");
        out.printCode("\tret");
    }

    /** Assigns Labels to any Instruction that might be the target of a
     * conditional or unconditional jump. */

    private HashMap<Instruction, String> assignLabels(Function f) {
        HashMap<Instruction, String> labelMap = new HashMap<>();
        Stack<Instruction> tovisit = new Stack<>();
        HashSet<Instruction> discovered = new HashSet<>();
        tovisit.push(f.getStart());
        while (!tovisit.isEmpty()) {
            Instruction inst = tovisit.pop();

            for (int childIdx = 0; childIdx < inst.numNext(); childIdx++) {
                Instruction child = inst.getNext(childIdx);
                if (discovered.contains(child)) {
                    //Found the node for a second time...need a label for merge points
                    if (!labelMap.containsKey(child)) {
                        labelMap.put(child, getNewLabel());
                    }
                } else {
                    discovered.add(child);
                    tovisit.push(child);
                    //Need a label for jump targets also
                    if (childIdx == 1 && !labelMap.containsKey(child)) {
                        labelMap.put(child, getNewLabel());
                    }
                }
            }
        }
        return labelMap;
    }

    public void visit(AddressAt i) {
        if(i.getOffset() == null) {
            out.bufferCode("\tmovq $0" + ", %rip");
            out.printCode("\tmovq $0" + ", %rip");
        }else {
            out.bufferCode("\tmovq " + convLocAddr(i.getOffset()) + ", %rip");
            out.printCode("\tmovq " + convLocAddr(i.getOffset()) + ", %rip");
        }
        out.bufferCode("\tmovq " + i.getBase().toString().replace("%","") + "@GOTPCREL(%rip) , %r11");
        out.bufferCode("\tmovq "  + " %r11, "+ convAddr(i.getDst()) + "(%rbp)");

        out.printCode("\tmovq " + i.getBase().toString().replace("%","") + "@GOTPCREL(%rip) , %r11");
        out.printCode("\tmovq "  + " %r11, "+ convAddr(i.getDst()) + "(%rbp)");
//        out.bufferCode("movq " + (i.getBase().getName()) + " %r11");
//        out.bufferCode("movq $" + (i.getOffset().toString()) + "%r10");
//        out.bufferCode("imul %r10, %r11");                 // Multiply offset by 8
//        out.bufferCode("movq " + (i.getBase().getName()) + "e@GOTPCREL(%rip) , %r10");  // Load array address
//        out.bufferCode(addq %r10, %r11);                 // Add array base address with offset
//        out.bufferCode(movq %r11, -368(%rbp));
    }

    public void visit(BinaryOperator i) {
        System.out.println(convLocAddr(i.getLeftOperand()));
        System.out.println(convLocAddr(i.getRightOperand()));
        System.out.println(convLocAddr(i.getDst()));
        if(i.getOperator().getClass() == BinaryOperator.Op.Add.getClass()){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%, %r10");
            out.bufferCode("\taddq " + convLocAddr(((LocalVar)i.getRightOperand())) + "%, %r10");
            out.bufferCode("\tmoveq " + "%r10, "+ convLocAddr(i.getDst()) + "(%rbp)");
            out.printCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%, %r10");
            out.printCode("\taddq " + convLocAddr(((LocalVar)i.getRightOperand())) + "%, %r10");
            out.printCode("\tmoveq " + "%r10, "+ convLocAddr(i.getDst()) + "(%rbp)");
        }else if(i.getOperator().getClass() == BinaryOperator.Op.Sub.getClass()){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%, %r10");
            out.bufferCode("\tsubq " + convLocAddr(((LocalVar)i.getRightOperand())) + "%, %r10");
            out.bufferCode("\tmoveq " + "%r10, "+ convLocAddr(i.getDst()) + "(%rbp)");
            out.printCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%, %r10");
            out.printCode("\tsubq " + convLocAddr(((LocalVar)i.getRightOperand())) + ", %r10");
            out.printCode("\tmoveq " + "%r10, "+ convLocAddr(i.getDst()) + "(%rbp)");
        }else if(i.getOperator().getClass() == BinaryOperator.Op.Mul.getClass()){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + ", %rax");
            out.bufferCode("\timulq " + convLocAddr(((LocalVar)i.getRightOperand())));
            out.bufferCode("\tmoveq " + "%rax, "+ convLocAddr(i.getDst()) + "(%rbp)");
            out.printCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + ", %rax");
            out.printCode("\timulq " + convLocAddr(((LocalVar)i.getRightOperand())));
            out.printCode("\tmoveq " + "%rax, "+ convLocAddr(i.getDst()) + "(%rbp)");
        }else if(i.getOperator().getClass() == BinaryOperator.Op.Div.getClass()){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + ", %rax");
            out.bufferCode("\taddq " + convLocAddr(((LocalVar)i.getRightOperand())));
            out.bufferCode("\tmoveq " + "%rax, "+ convLocAddr(i.getDst()) + "(%rbp)");
            out.printCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%rax");
            out.printCode("\taddq " + convLocAddr(((LocalVar)i.getRightOperand())));
            out.printCode("\tmoveq " + "%rax, "+ convLocAddr(i.getDst()) + "(%rbp)");
        }

    }

    public void visit(CompareInst i) {
        out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%r10");
        out.bufferCode("\tcmp " + " %r10, " + convLocAddr(((LocalVar)i.getRightOperand())));

        out.printCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + "%r10");
        out.printCode("\tcmp " + " %r10, " + convLocAddr(((LocalVar)i.getRightOperand())));
//        if(i.getPredicate() == CompareInst.Predicate.EQ) {
//
//        }else if(i.getPredicate() == CompareInst.Predicate.GE) {
//
//        }else if(i.getPredicate() == CompareInst.Predicate.GT) {
//
//        }else if(i.getPredicate() == CompareInst.Predicate.LE) {
//
//        }else if(i.getPredicate() == CompareInst.Predicate.LT) {
//
//        }else if(i.getPredicate() == CompareInst.Predicate.NE) {
//
//        }
    }

    public void visit(CopyInst i) {
        out.bufferCode("\tmovq " + irFormat.apply(i.getSrcValue()) + ", %r10");
        out.bufferCode("\tcmp " + " %r10, " + convLocAddr(((LocalVar)i.getDstVar())) + "(%rbp)");

        out.printCode("\tmovq " + irFormat.apply(i.getSrcValue()) + ", %r10");
        out.printCode("\tcmp " + " %r10, " + convLocAddr(((LocalVar)i.getDstVar())) + "(%rbp)");
    }

    public void visit(JumpInst i) {
        out.bufferCode("\tmoveq " + convLocAddr(i.getPredicate()) + ", %r10");
        out.bufferCode("\tcmp 1,"  + "%r10");
        out.bufferCode("\tjmp ");

        out.printCode("\tmoveq " + convLocAddr(i.getPredicate()) + ", %r10");
        out.printCode("\tcmp 1,"  + "%r10");
        out.printCode("\tjmp ");

        System.out.println(convLocAddr(i.getPredicate()));
    }

    public void visit(LoadInst i) {
        out.bufferCode("\tmoveq " + convAddr(i.getSrcAddress()) + ", %r10" );
        out.bufferCode("\tmoveq %r10, " + convLocAddr(i.getDst()));

        out.printCode("\tmoveq " + convAddr(i.getSrcAddress()) + ", %r10" );
        out.printCode("\tmoveq %r10, " + convLocAddr(i.getDst()));
    }

    public void visit(NopInst i) {
    }

    public void visit(StoreInst i) {
        out.bufferCode("\tmoveq " + convLocAddr(i.getSrcValue()) + "%r10");
        out.bufferCode("\tmoveq %r10, " + convAddr(i.getDestAddress()));

        out.printCode("\tmoveq " + convLocAddr(i.getSrcValue()) + "%r10");
        out.printCode("\tmoveq %r10, " + convAddr(i.getDestAddress()));
    }

    public void visit(ReturnInst i) {
        out.bufferCode("\tmoveq " + convLocAddr(i.getReturnValue()) + ", %r10");
        out.bufferCode("\tmoveq %r10, %rax" );
        out.bufferCode("\tleave");
        out.bufferCode("\tret");

        out.printCode("\tmoveq " + convLocAddr(i.getReturnValue()) + ", %r10");
        out.printCode("\tmoveq %r10, %rax" );
        out.printCode("\tleave");
        out.printCode("\tret");
    }

    public void visit(CallInst i) {
        out.bufferCode("\tmoveq " + convLocAddr(i.getDst())+   "(%rdp), " + "%rdi");
        out.bufferCode("\tcall " + (i.getCallee()) );

        out.printCode("\tmoveq " + convLocAddr(i.getDst())+   "(%rdp), " + "%rdi");
        out.printCode("\tcall " + (i.getCallee()) );
    }

    public void visit(UnaryNotInst i) {
    }
}
