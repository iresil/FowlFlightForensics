package FowlFlightForensics.domain;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Deprecated
@NoArgsConstructor
public class TopNContainer {
    public List<IncidentResult> top = new ArrayList<>();

    public Object[] getArray() {
        return top.toArray();
    }

    public void add(IncidentResult item) {
        top.add(item);
    }
}
