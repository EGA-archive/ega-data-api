package eu.elixir.ega.ebi.downloader.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "dataset")
public class Dataset {
    @Id
    @Size(max = 128)
    @Column(name = "dataset_id", insertable = false, updatable = false, length = 128)
    private String datasetId;
    
    @Size(max = 256)
    @Column(name = "description", insertable = false, updatable = false, length = 256)
    private String description;
    
    @Size(max = 128)
    @Column(name = "dac_stable_id", insertable = false, updatable = false, length = 128)
    private String dacStableId;
    
    @Size(max = 3)
    @Column(name = "double_signature", insertable = false, updatable = false, length = 3)
    private String doubleSignature;
}
