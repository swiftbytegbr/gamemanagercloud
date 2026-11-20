package de.swiftbyte.gmc.backend.aop;

import de.swiftbyte.gmc.common.model.NodeTask;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate server-level methods to block execution when there are active node tasks
 * that affect the server or the whole node (node-global tasks).
 *
 * - Checks only tasks on the server's node.
 * - A task blocks if it either targets the server (targetIds contains serverId)
 *   or is node-global (targetIds is null/empty).
 * - If {@code all} is true, any task type will block; otherwise only the provided types block.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockOnNodeTasksForServer {

    /** If true, all task types block the action. */
    boolean all() default false;

    /** Specific task types that block the action (ignored if {@code all} == true). */
    NodeTask.Type[] types() default {};
}

