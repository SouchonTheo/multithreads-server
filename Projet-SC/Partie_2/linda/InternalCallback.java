package linda;

public class InternalCallback {
    
    private Tuple template;

    private Callback callback;

    public InternalCallback(Tuple tuple, Callback cb) {
        this.callback = cb;
        this.template = tuple;
    }

    public Tuple getTemplate() {
        return template;
    }

    public Callback getCallback() {
        return callback;
    }
}
