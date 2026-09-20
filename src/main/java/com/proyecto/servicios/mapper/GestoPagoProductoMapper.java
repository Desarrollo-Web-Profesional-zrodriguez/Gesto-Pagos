package com.proyecto.servicios.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;

@Mapper(componentModel = "spring")
public interface GestoPagoProductoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    GestoPagoProducto toEntity(ProductItemDto dto);

    List<GestoPagoProducto> toEntityList(List<ProductItemDto> doList);
    ProductItemDto toDto(GestoPagoProducto entity);
    List<ProductItemDto> toDtoList(List<GestoPagoProducto> entityList);
}