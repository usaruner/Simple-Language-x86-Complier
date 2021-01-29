package crux.frontend.types;

public abstract class Type {
    Type add(Type that) {
        if (this.getClass() == IntType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot add " + this + " with " + that);
    }

    Type sub(Type that) {
        if (this.getClass() == IntType.class &&this.equivalent(that))
            return that;
        return new ErrorType("cannot subtract " + this + " from " + that);
    }

    Type mul(Type that) {
        if (this.getClass() == IntType.class &&this.equivalent(that))
            return that;
        return new ErrorType("cannot multiply " + this + " with " + that);
    }

    Type div(Type that) {
        if (this.getClass() == IntType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot divide " + this + " by " + that);
    }

    Type and(Type that) {
        if (this.getClass() == BoolType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot compute " + this + " and " + that);
    }

    Type or(Type that) {
        if (this.getClass() == BoolType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot compute " + this + " or " + that);
    }

    Type not() {
        if (this.getClass() == BoolType.class)
            return this;
        return new ErrorType("cannot negate " + this);
    }

    Type compare(Type that) {
        if (this.getClass() == IntType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot compare " + this + " with " + that);
    }

    Type deref() {
        if(this.getClass() != AddressType.class)
            return this;
        return new ErrorType("cannot deref " + this);
    }

    Type index(Type that) {
        if (this.getClass() == IntType.class && this.equivalent(that))
            return that;
        return new ErrorType("cannot index " + this + " with " + that);
    }

    Type call(Type args) {
        if (args.getClass() == IntType.class || args.getClass() == BoolType.class)
            return args;
        return new ErrorType("cannot call " + this + " using " + args);
    }

    Type assign(Type source) {
        if (this.equivalent(source) && this.getClass() != VoidType.class)
            return source;
        return new ErrorType("cannot assign " + source + " to " + this);
    }

    public boolean equivalent(Type that) {
        throw new Error(this.getClass() + "should override the equivalent method.");
    }
}
