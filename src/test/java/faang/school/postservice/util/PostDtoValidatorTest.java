package faang.school.postservice.util;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.validation.PostDtoValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostDtoValidatorTest {
    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintViolationBuilder violationBuilder;

    @InjectMocks
    private PostDtoValidator validator;

    private PostDto postDto;

    @BeforeEach
    public void setUp() {
        postDto = new PostDto(1L, "qwe", null, null, null);
    }

    @Test
    public void isValid_NeitherAuthorNorProjectSpecified() {
        postDto.setAuthorId(null);
        postDto.setProjectId(null);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);

        boolean result = validator.isValid(postDto, context);

        assertFalse(result);
    }

    @Test
    public void isValid_BothAuthorAndProjectSpecified() {
        postDto.setAuthorId(1L);
        postDto.setProjectId(1L);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);

        boolean result = validator.isValid(postDto, context);

        assertFalse(result);
    }

    @Test
    public void isValid_OnlyAuthorSpecified() {
        postDto.setAuthorId(1L);
        postDto.setProjectId(null);

        boolean result = validator.isValid(postDto, context);

        assertTrue(result);
    }

    @Test
    public void isValid_OnlyProjectSpecified() {
        postDto.setAuthorId(null);
        postDto.setProjectId(1L);

        boolean result = validator.isValid(postDto, context);

        assertTrue(result);
    }
}
