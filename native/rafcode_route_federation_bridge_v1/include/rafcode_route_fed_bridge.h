#ifndef RAFCODE_ROUTE_FED_BRIDGE_H
#define RAFCODE_ROUTE_FED_BRIDGE_H

#include "rafcode_route.h"
#include "rafcode_federation.h"

#define RAF_ROUTE_FED_BRIDGE_VERSION 0x0001u

#define RAF_ROUTE_FED_ERROR_ROUTE_MAGIC 0x00000001u
#define RAF_ROUTE_FED_ERROR_ROUTE_VERSION 0x00000002u
#define RAF_ROUTE_FED_ERROR_ROUTE_STATUS 0x00000004u
#define RAF_ROUTE_FED_ERROR_ROUTE_CODE 0x00000008u
#define RAF_ROUTE_FED_ERROR_ROUTE_MAPPING 0x00000010u
#define RAF_ROUTE_FED_ERROR_ROUTE_TAG 0x00000020u

raf_u32 raf_route_fed_digest(const raf_route_receipt *route, raf_u32 digest[4]);

#endif
