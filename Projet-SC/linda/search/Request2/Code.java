package linda.search.Request2;

public enum Code {
    Request, // Request, UUID, String
    Value, // Value, String //NEW : Value, UUID, String
    Result, // Result, UUID, String, Int
    Searcher, // Result, "done", UUID

    IncSearchers, // IncSearchers, UUID
}
