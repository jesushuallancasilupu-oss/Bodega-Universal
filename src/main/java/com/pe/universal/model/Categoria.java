
package com.pe.universal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Short idCategoria;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
        
}
