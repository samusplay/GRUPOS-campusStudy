package com.campusstudy.grupos.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
//clase para avsiarle a sping que existe el reolver
public class UsuarioAutenticadoResolver implements HandlerMethodArgumentResolver {

    // ¿Este parámetro es mío? Solo si tiene @UsuarioAutenticado y es Long
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioAutenticado.class)
                && Long.class.equals(parameter.getParameterType());
    }

    // Toma el usuario_id que dejó el JwtAuthFilter
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        Object usuarioId = webRequest.getAttribute(JwtAuthFilter.USUARIO_ID, RequestAttributes.SCOPE_REQUEST);

        if (usuarioId == null) {
            // Solo pasa si la ruta no pasó por el filtro: es un error de programación, no del cliente
            throw new IllegalStateException("No hay usuario autenticado en la petición");
        }
        return usuarioId;
    }
}