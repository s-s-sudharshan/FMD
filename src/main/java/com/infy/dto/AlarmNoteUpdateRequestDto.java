package com.infy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The alarm id comes from the URL path; an empty string is allowed so notes can be cleared. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmNoteUpdateRequestDto {

    @NotNull(message = "{alarm.notes.absent}")
    @Size(max = 1000, message = "{alarm.notes.tooLong}")
    private String notes;
}
