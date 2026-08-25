ENTRY(_start)

PHDRS
{
    image PT_LOAD FILEHDR PHDRS FLAGS(5);
    stack PT_GNU_STACK FLAGS(6);
}

SECTIONS
{
    . = RAF_IMAGE_BASE + SIZEOF_HEADERS;

    .text : ALIGN(16)
    {
        KEEP(*(.text._start))
        *(.text .text.*)
        *(.rodata .rodata.*)
    } :image

    .data : ALIGN(4)
    {
        *(.data .data.*)
    } :image

    .bss : ALIGN(4)
    {
        *(.bss .bss.*)
        *(COMMON)
    } :image

    .relocations : ALIGN(4)
    {
        *(.rel .rel.*)
        *(.rela .rela.*)
        *(.got .got.*)
        *(.plt .plt.*)
        *(.iplt .iplt.*)
    } :image

    /DISCARD/ :
    {
        *(.comment)
        *(.note .note.*)
        *(.eh_frame .eh_frame.*)
        *(.gcc_except_table .gcc_except_table.*)
        *(.ARM.attributes)
        *(.ARM.exidx .ARM.exidx.*)
        *(.ARM.extab .ARM.extab.*)
        *(.gnu.attributes)
    }
}

ASSERT(SIZEOF(.data) == 0, "rafcode federation forbids writable static data")
ASSERT(SIZEOF(.bss) == 0, "rafcode federation forbids BSS/global state")
ASSERT(SIZEOF(.relocations) == 0, "rafcode federation forbids runtime relocations")
