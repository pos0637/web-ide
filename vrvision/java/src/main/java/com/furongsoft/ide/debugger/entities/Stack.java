package com.furongsoft.ide.debugger.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 调用堆栈
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stack {
    /**
     * 调用栈
     */
    List<Location> locations;

    /**
     * 清空
     */
    public void clear() {
        if (locations != null) {
            locations.clear();
        }
    }
}
