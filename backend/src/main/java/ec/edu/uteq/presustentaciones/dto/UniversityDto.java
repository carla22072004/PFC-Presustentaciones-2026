package ec.edu.uteq.presustentaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String country;
    
    private List<String> domains;

    @JsonProperty("web_pages")
    private List<String> webPages;
}
