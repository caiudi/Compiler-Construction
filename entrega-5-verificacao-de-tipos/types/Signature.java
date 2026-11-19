package types;

import java.util.List;
import java.util.Objects;

/** Assinatura de funcao ou procedimento: tipos dos parametros e tipo de retorno (VOID para procedimento). */
public final class Signature {

    public final List<Type> parameters;
    public final Type result;

    public Signature(List<Type> parameters, Type result) {
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        this.result = Objects.requireNonNull(result, "result");
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) s.append(", ");
            s.append(parameters.get(i));
        }
        return s.append(") -> ").append(result).toString();
    }
}
