package eu.elixir.ega.ebi.commons.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Dataset {
    private String datasetId;
    private String description;
    private String dacStableId;
    private String doubleSignature;
}
