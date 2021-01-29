package crux.frontend.types;

public final class IntType extends Type {
    @Override
    public String toString() {
        return "int";
    }
    @Override
    public boolean equivalent(Type that) {
        return that.getClass() == IntType.class;
    }
}
