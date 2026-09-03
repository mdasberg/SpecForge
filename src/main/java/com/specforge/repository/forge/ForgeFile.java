package com.specforge.repository.forge;

/**
 * One file as the forge holds it.
 *
 * @param author the last commit author for this path, which becomes the document's owner
 */
public record ForgeFile(String path, String content, String commitSha, String author) {}
