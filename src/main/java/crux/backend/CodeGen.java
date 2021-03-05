package crux.backend;

import crux.frontend.types.IntType;
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
    private StringBuffer output = new StringBuffer();
    private HashMap<String, String> var = new HashMap<>();
    private HashMap<Instruction, String> jumpMap;
    private int numOfVar = 0;
    private int numOftemp = 0;
    private int numOfGlob = 0;
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
        int size;
        Function func;
        int tep = 0;
        while(globV.hasNext()&& tep  < 500){
            temp = (GlobalDecl)globV.next();
            name = temp.getAllocatedAddress().getName().replace("%","");
            size = 8*Integer.parseInt(irFormat.apply(temp.getNumElement()));
            out.bufferCode(".comm " + name + ", " + size + ", " + codeSize);

            //System.out.println("SIZE: " + ".comm " + name + ", " + irFormat.apply(size) + ", " + codeSize);
            tep++;
        }
        tep = 0;
        while(globF.hasNext() && tep  < 500){
            func = (Function)globF.next();
            genCode(func);
            tep++;
            System.out.print("tep:" + var);
            var.clear();
        }
        out.close();
    }

    private int labelcount = 1;

    private String getNewLabel() {
        return "L" + (labelcount++);
    }
    private String convLocAddr(LocalVar t){

        boolean isNum = false;
        try {
            Integer.parseInt(t.toString().substring(2,t.toString().length()));
            isNum = true;
        } catch(NumberFormatException e){
            isNum = false;
        }
        if(isNum) {
            int num = (8 * (1 + Integer.parseInt(t.toString().replace("$t", ""))));
            return ("-" + num + "(%rbp) ");
        }
        return("var:" + t.toString().substring(1,t.toString().length()));
    }
    private String convAddr(AddressVar t) {
        int num = Integer.parseInt(t.toString().replace("%t",""));
        if(num > numOfGlob - 1)
            numOfGlob = num + 1;
        return ("-" + 8*(numOftemp + 1 + num) + "(%rbp) ");
    }

    private void visit(Instruction inst,int add){
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
        String temp = out.sb.toString();
        temp = temp.replace("$t","");
        if(temp.contains("var:")) {
            temp = temp.substring(temp.indexOf("var:"), temp.length());
            //System.out.println("five" + temp);
            temp = temp.substring(0, temp.indexOf(" "));
            if(!var.containsKey(temp)){
                var.put(temp,"-" + 8*(numOfVar + add) + "(%rbp)");
                numOfVar++;
            }
//            System.out.println("TEMP: " + temp);
        }


        output.append(out.sb);
        out.sb.delete(0,out.sb.length());
    }
    private void genCode(Function f) {
        int tempNum = Integer.parseInt(f.getTempVar(new IntType()).toString().replace("$t",""));
//        System.out.println("ARGS: " + f.getArguments().size());
        numOftemp = (f.getArguments().size() + tempNum);
//        System.out.println(tempNum);
        //Assign labels for jump targets
        jumpMap = assignLabels(f);
        //Declare functions and print label    “.globl main”      “main:”
//        System.out.println("Name "+ f.getName() );
        out.bufferLabel(".globl " + f.getName());
        //print label
        out.bufferLabel(f.getName() + ":");
        //Emit functions prologue

        out.bufferCode("\tenter $(UNKNOWN)" + ", $0");

        //For functions Arguments
        if(f.getArguments().size() > 6) {
            for (int i = 5; i < f.getArguments().size(); i++) {
                out.bufferCode("\tmovq " + convLocAddr((LocalVar) f.getArguments().get(i))  + ", " + (-8*(i+1)) + "(%rbp)");
            }
        }
        for(int i = 0; i < f.getArguments().size(); i++){
            if(f.getArguments().size() <= 5) {
                out.bufferCode("\tmovq " + InitArgs[i] + ", " + convLocAddr((LocalVar) f.getArguments().get(i)) + " ");
            }
        }
        Instruction inst = f.getStart();
        int tep = 0;
        while(inst != null && tep < 500){
            visit(inst,(f.getArguments().size()+2 + tempNum -1));
            if(jumpMap.containsKey(inst)){
                out.bufferCode("\tjmp " + jumpMap.get(inst));
                output.append(out.sb);
                out.sb.delete(0,out.sb.length());
                break;
            }
            inst = inst.getNext(0);
            tep++;
            if(inst == null){
                out.bufferCode("\tleave");
                out.bufferCode("\tret");
                output.append(out.sb);
                out.sb.delete(0,out.sb.length());
            }
        }
        Instruction key;
        String lab;
        System.out.println(jumpMap);
        for (Map.Entry< Instruction, String> entry : jumpMap.entrySet()) {
            key = entry.getKey();
            lab = entry.getValue();
            System.out.println(key + lab);
            out.bufferLabel(lab + ":");
            tep = 0;

            while(key != null && tep < 500){
                System.out.println("lab:" + key);
                visit(key,(f.getArguments().size()+2 + tempNum -1));
                key = key.getNext(0);
                tep++;
                if(key == null){
                    out.bufferCode("\tleave");
                    out.bufferCode("\tret");
                    output.append(out.sb);
                    out.sb.delete(0,out.sb.length());
                }
                if(jumpMap.containsKey(key)){
                    out.bufferCode("\tjmp " + jumpMap.get(key));
                    output.append(out.sb);
                    out.sb.delete(0,out.sb.length());
                    break;
                }
            }
        }

        output = output.insert(output.indexOf("(UNKNOWN)"), "(8*" + (numOftemp+numOfGlob+numOfVar+2));
        output = output.replace(output.indexOf("(UNKNOWN)"),output.indexOf("(UNKNOWN)") + 8,"");
        String variable;
        String repl;
        String buf = output.toString();
        for (Map.Entry< String,String> entry : var.entrySet()) {
            variable = entry.getKey();
            repl = entry.getValue();
            buf = buf.replace(variable,repl);
        }
//        System.out.println("hill" + jumpMap);
        output.delete(0,output.length());
        output.append(buf);


//        System.out.println(numOfVar);
//        System.out.println(var);
        out.sb = output;

        System.out.println("Glob = " + numOfGlob);
        System.out.println("Temp = " + numOftemp);
        System.out.println("Var = " + numOfVar);
        System.out.println(out.sb);
        out.outputBuffer();
        numOftemp = 0;
        output.delete(0,output.length());
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
//        if(i.getOffset() == null) {
//            out.bufferCode("\tmovq $0" + ", %rip");
//            out.printCode("\tmovq $0" + ", %rip");
//        }else {
//            out.bufferCode("\tmovq " + convLocAddr(i.getOffset()) + ", %rip");
//            out.printCode("\tmovq " + convLocAddr(i.getOffset()) + ", %rip");
//        }
        System.out.println("ADDR");
        out.bufferCode("/* AddressAt: */");
        if(i.getOffset() == null) {
            System.out.println("ADDR1");
            out.bufferCode("\tmovq " + i.getBase().toString().replace("%", "") + "@GOTPCREL(%rip)" + ", %r10");
            out.bufferCode("\tmovq %r10, " + convAddr(i.getDst()) + " ");
        }else{
            System.out.println("ADDR2");
            out.bufferCode("\tmovq " + convLocAddr((LocalVar) i.getOffset())+ " , %r11 ");
            out.bufferCode("\tmovq $8 , %r10 ");
            out.bufferCode("\timul %r10, %r11");
            out.bufferCode("\tmovq " + i.getBase().toString().replace("%", "") + "@GOTPCREL(%rip)" + ", %r10");
            out.bufferCode("\taddq %r10, %r11");
            out.bufferCode("\tmovq %r11, " + convAddr(i.getDst()) + " ");
        }
    }

    public void visit(BinaryOperator i) {
//        System.out.println(convLocAddr(i.getLeftOperand()));
//        System.out.println(convLocAddr(i.getRightOperand()));
//        System.out.println(i.getOperator());
        out.bufferCode("/* binaryOp: */");
        if(i.getOperator() == BinaryOperator.Op.Add){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + " , %r10");
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getRightOperand())) + " , %r11");
            out.bufferCode("\taddq %r11 , %r10");
            out.bufferCode("\tmovq " + "%r10, "+ convLocAddr(i.getDst()) + " " );

        }else if(i.getOperator() == BinaryOperator.Op.Sub){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + " , %r10");
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getRightOperand())) + " , %r11");
            out.bufferCode("\tsubq %r11 , %r10");
            out.bufferCode("\tmovq " + "%r10, "+ convLocAddr(i.getDst()) + " " );
        }else if(i.getOperator() == BinaryOperator.Op.Mul){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + " , %r10");
            out.bufferCode("\tmovq %r10, %rax");
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getRightOperand())) + " , %r11");
            out.bufferCode("\tmulq %r11 ");
            out.bufferCode("\tmovq " + "%rax, "+ convLocAddr(i.getDst()) + " " );
        }else if(i.getOperator() == BinaryOperator.Op.Div){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + " , %r10");
            out.bufferCode("\tmovq %r10, %rax");
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getRightOperand())) + " , %r11");
            out.bufferCode("\tdivq %r11 ");
            out.bufferCode("\tmovq " + "%rax, "+ convLocAddr(i.getDst()) + " " );
        }

    }

    public void visit(CompareInst i) {
//        System.out.println("hello" + i.getPredicate().name());
        out.bufferCode("/* CompareInst: */");
        out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getLeftOperand())) + ", %r10");
        out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getRightOperand())) + ", %r11");
        out.bufferCode("\tcmp " + " %r11, %r10 " );
        out.bufferCode("\tmovq $0, %r10 " );
        out.bufferCode("\tmovq $1, %r11 " );
        if(i.getPredicate() == CompareInst.Predicate.EQ) {
            out.bufferCode("\tcmove %r11,  %r10 " );
        }else if(i.getPredicate() == CompareInst.Predicate.GE) {
            out.bufferCode("\tcmovge %r11, %r10 " );
        }else if(i.getPredicate() == CompareInst.Predicate.GT) {
            out.bufferCode("\tcmovg %r11, %r10 " );
        }else if(i.getPredicate() == CompareInst.Predicate.LE) {
            out.bufferCode("\tcmove %r11, %r10 " );
        }else if(i.getPredicate() == CompareInst.Predicate.LT) {
            out.bufferCode("\tcmovl %r11, %r10 " );
        }else if(i.getPredicate() == CompareInst.Predicate.NE) {
            out.bufferCode("\tcmovne %r11, %r10 " );
        }
        out.bufferCode("\tmovq " + " %r10, " + convLocAddr(i.getDst()) + " " );
    }

    public void visit(CopyInst i) {
        out.bufferCode("/* CopyInst */");
        if(irFormat.apply(i.getSrcValue()).equals("true")) {
            out.bufferCode("\tmovq $1," + " %r10");
            out.bufferCode("\tmovq " + " %r10, " + convLocAddr(((LocalVar) i.getDstVar()))+ " " );

        }else if(irFormat.apply(i.getSrcValue()).equals("false")){
            out.bufferCode("\tmovq $0, " + " %r10");
            out.bufferCode("\tmovq " + " %r10, " + convLocAddr(((LocalVar) i.getDstVar()))+ " " );

        }else if (i.getSrcValue().getClass() == AddressVar.class){
            out.bufferCode("\tmovq " + convAddr(((AddressVar)i.getSrcValue())) + " , %r10");
            out.bufferCode("\tmovq " + " %r10, " + convLocAddr(((LocalVar) i.getDstVar()))+ " " );

        }else if (i.getSrcValue().getClass() == LocalVar.class){
            out.bufferCode("\tmovq " + convLocAddr(((LocalVar)i.getSrcValue())) + " , %r10");
            out.bufferCode("\tmovq " + " %r10, " + convLocAddr(((LocalVar) i.getDstVar()))+ " " );

        }else{
            out.bufferCode("\tmovq $" + irFormat.apply(i.getSrcValue())+ " , %r10");
            out.bufferCode("\tmovq " + " %r10, " + convLocAddr(((LocalVar) i.getDstVar()))+ " " );

        }

    }

    public void visit(JumpInst i) {
        out.bufferCode("/* JumpInst */");
//        System.out.println("Jump :" + i.getNext(0) + i.getNext(1));
        out.bufferCode("\tmovq " + convLocAddr(i.getPredicate()) + " , %r10");

//        if(i.getPredicate().toString())
        if(jumpMap.containsKey(i.getNext(0))) {
            out.bufferCode("\tcmp $0,"  + "%r10");
            out.bufferCode("\tje " + jumpMap.get(i.getNext(0)));
        }
        if(jumpMap.containsKey(i.getNext(1))){
            out.bufferCode("\tcmp $1,"  + "%r10");
            out.bufferCode("\tje " + jumpMap.get(i.getNext(1)));
        }
    }

    public void visit(LoadInst i) {
        out.bufferCode("/* LoadInst */");
        out.bufferCode("\tmovq " + convAddr(i.getSrcAddress()) + " , %r10" );
        out.bufferCode("\tmovq (%r10), %r11" );
        out.bufferCode("\tmovq %r11, " + convLocAddr(i.getDst())+ " " );

    }

    public void visit(NopInst i) {
    }

    public void visit(StoreInst i) {
        out.bufferCode("/* StoreInst */");
//        System.out.println("Store: " + i.getDestAddress());
        out.bufferCode("\tmovq " + convLocAddr(i.getSrcValue()) + " , %r10");
        out.bufferCode("\tmovq " + convAddr(i.getDestAddress()) + " , %r11");
        out.bufferCode("\tmovq %r10, (%r11)");

    }

    public void visit(ReturnInst i) {
        out.bufferCode("/* ReturnInst */");
        out.bufferCode("\tmovq " + convLocAddr(i.getReturnValue()) + " , %r10");
        out.bufferCode("\tmovq %r10, %rax" );
        out.bufferCode("\tleave");
        out.bufferCode("\tret");

    }

    public void visit(CallInst i) {
        out.bufferCode("/* CallInst */");
        if(i.getParams().size() > 6) {
            for (int j = 5; j < i.getParams().size(); j++) {
                out.bufferCode("\tmovq " + convLocAddr((LocalVar) i.getParams().get(j))+ " , " + (-8*(j+1)) + "(%rbp)");
            }
        }
        for(int j = 0; j < i.getParams().size(); j++){
            if(i.getParams().size() <= 5) {
                out.bufferCode("\tmovq " + convLocAddr((LocalVar)i.getParams().get(j)) + " , " + InitArgs[j]);
            }
        }
        out.bufferCode("\tcall " + (i.getCallee().toString().replace("%","")) );
        out.bufferCode("\tmovq %rax, " + convLocAddr(i.getDst()) + " ");
    }

    public void visit(UnaryNotInst i) {
    }
}
